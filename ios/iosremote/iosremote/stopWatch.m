//
//  stopWatch.m
//  iosremote
//
//  Created by Siqi Liu on 7/29/13.
//  Copyright (c) 2013 libreoffice. All rights reserved.
//

#import "stopWatch.h"

@interface stopWatch ()

@property NSTimeInterval lastInterval;
@property int state;
@property (weak, nonatomic) UIButton * startButton;
@property (weak, nonatomic) UIButton * clearButton;
@property (weak, nonatomic) UILabel * timeLabel;

@end

@implementation stopWatch
@synthesize startButton = _startButton;
@synthesize clearButton = _clearButton;
@synthesize timeLabel = _timeLabel;

@synthesize lastInterval = _lastInterval;

- (stopWatch *) init
{
    self = [super init];
    self.state = TIMER_STATE_CLEARED;
    self.set = NO;
    return self;
}

- (stopWatch *) initWithStartButton:(UIButton *)startButton
                        ClearButton:(UIButton *)clearButton
                          TimeLabel:(UILabel *)timeLabel
{
    self = [self init];
    
    self.startButton = startButton;
    self.clearButton = clearButton;
    self.timeLabel = timeLabel;
    
    [self setupActions];
    return self;
}

- (void) setupWithTableViewCell:(UITableViewCell *)cell
{
    self.startButton = (UIButton *)[cell viewWithTag:2];
    self.clearButton = (UIButton *)[cell viewWithTag:3];
    self.timeLabel = (UILabel *)[cell viewWithTag:1];
    
    [self setupActions];
}

- (void) setupActions
{
    [self.startButton addTarget:self action:@selector(start) forControlEvents:UIControlEventTouchUpInside];
    [self.clearButton addTarget:self action:@selector(clear) forControlEvents:UIControlEventTouchUpInside];
    self.set = YES;
}

- (void)updateTimer
{
    // Create date from the elapsed time
    NSDate *currentDate = [NSDate date];
    NSTimeInterval timeInterval = [currentDate timeIntervalSinceDate:self.startDate] + self.lastInterval;
    NSDate *timerDate = [NSDate dateWithTimeIntervalSince1970:timeInterval];
    
    // Create a date formatter
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"HH:mm:ss"];
    [dateFormatter setTimeZone:[NSTimeZone timeZoneForSecondsFromGMT:0.0]];
    
    // Format the elapsed time and set it to the label
    NSString *timeString = [dateFormatter stringFromDate:timerDate];
    self.timeLabel.text = timeString;
}


- (void) start
{
    switch (self.state) {
        case TIMER_STATE_RUNNING:
            self.state = TIMER_STATE_PAUSED;
            [self.stopWatchTimer invalidate];
            self.lastInterval += [[NSDate date] timeIntervalSinceDate:self.startDate];
            break;
        case TIMER_STATE_PAUSED:
            self.state = TIMER_STATE_RUNNING;
            self.startDate = [NSDate date];
            self.stopWatchTimer = [NSTimer scheduledTimerWithTimeInterval:1.0/10.0
                                                                   target:self
                                                                 selector:@selector(updateTimer)
                                                                 userInfo:nil
                                                                  repeats:YES];
            break;
        case TIMER_STATE_CLEARED:
            self.state = TIMER_STATE_RUNNING;
            self.startDate = [NSDate date];
            // Create the stop watch timer that fires every 100 ms
            self.stopWatchTimer = [NSTimer scheduledTimerWithTimeInterval:1.0/10.0
                                                                   target:self
                                                                 selector:@selector(updateTimer)
                                                                 userInfo:nil
                                                                  repeats:YES];
            break;
        default:
            break;
    }
    
    [self updateStartButtonIcon];
}

- (void) updateStartButtonIcon
{
    switch (self.state) {
        case TIMER_STATE_RUNNING:
            [self.startButton setImage:[UIImage imageNamed:@"timer_pause_btn"] forState:UIControlStateNormal];
            break;
        case TIMER_STATE_PAUSED:
            [self.startButton setImage:[UIImage imageNamed:@"timer_resume_btn"] forState:UIControlStateNormal];
            break;
        case TIMER_STATE_CLEARED:
            [self.startButton setImage:[UIImage imageNamed:@"timer_start_btn"] forState:UIControlStateNormal];
            break;
        default:
            break;
    }
}

- (void) clear
{
    [self.stopWatchTimer invalidate];
    self.stopWatchTimer = nil;
    self.startDate = [NSDate date];
    self.lastInterval = 0;
    self.state = TIMER_STATE_CLEARED;
    
    [self.startButton setImage:[UIImage imageNamed:@"timer_start_btn"] forState:UIControlStateNormal];
    [self updateTimer];
}

@end

