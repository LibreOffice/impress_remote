// -*- Mode: ObjC; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
//
// This file is part of the LibreOffice project.
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

#import "serverList_vc.h"
#import "CommunicationManager.h"
#import "newServer_vc.h"
#import "Server.h"
#import "Client.h"
#import "ControlVariables.h"

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

@interface server_list_vc () <NSNetServiceBrowserDelegate, NSNetServiceDelegate>

@property (nonatomic, strong) CommunicationManager *comManager;
@property (nonatomic, weak) NSNotificationCenter* center;
@property (nonatomic, strong) id slideShowPreviewStartObserver;
@property (nonatomic, strong) id pinValidationObserver;
@property (atomic, strong) NSIndexPath *lastSpinningCellIndex;

@property (nonatomic, strong) NSNetServiceBrowser *serviceBrowser;
@property (nonatomic, strong) NSTimer *searchLabelTimer;
@property (nonatomic, strong) NSTimer *searchTimeoutTimer;
@property (nonatomic, strong) NSString *searchStateText;
@property (nonatomic)  UITableViewCellSelectionStyle style;

@end

@implementation server_list_vc

@synthesize style = _style;
@synthesize searchStateText = _searchStateText;
@synthesize searchLabelTimer = _searchLabelTimer;
@synthesize center = _center;
@synthesize comManager = _comManager;
@synthesize lastSpinningCellIndex = _lastSpinningCellIndex;
@synthesize slideShowPreviewStartObserver = _slideShowPreviewStartObserver;
@synthesize pinValidationObserver = _pinValidationObserver;
@synthesize serviceBrowser = _serviceBrowser;

#pragma mark - helper
- (void) setSearchStateText:(NSString *)searchStateText
{
    _searchStateText = searchStateText;
    [self.serverTable reloadSections:[NSIndexSet indexSetWithIndex:0] withRowAnimation:UITableViewRowAnimationNone];
}

- (void) setStyle:(UITableViewCellSelectionStyle)style
{
    _style = style;
    [self.serverTable reloadSections:[NSIndexSet indexSetWithIndex:0] withRowAnimation:UITableViewRowAnimationNone];
}

#pragma mark - netservice resolve delegate
-(void) netServiceDidResolveAddress:(NSNetService *)sender
{
    if ([sender.addresses count]){
        NSData * address = [[sender addresses] objectAtIndex: 0];
        struct sockaddr_in *socketAddress = (struct sockaddr_in *) [address bytes];
        NSString * ipString = [NSString stringWithFormat: @"%s",inet_ntoa(socketAddress->sin_addr)];
        int port = socketAddress->sin_port;
        NSLog(@"Resolved at %@:%u", ipString, port);
        
        [self.comManager connectToServer:[[Server alloc] initWithProtocol:NETWORK atAddress:ipString ofName:sender.name]];
    }
}

-(void) netService:(NSNetService *)sender didNotResolve:(NSDictionary *)errorDict
{
    NSLog(@"Failed to resolve");
    UIAlertView *message = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"Failed to reach your computer", nil)
                                                      message:NSLocalizedString(@"Please consider restart LibreOffice Impress on your computer.", nil)
                                                     delegate:self
                                            cancelButtonTitle:NSLocalizedString(@"OK", nil)
                                            otherButtonTitles:nil];
    [self disableSpinner];
    [message show];
}

#pragma mark - bonjour service discovery

-(void) handleSearchTimeout
{
    [self.serviceBrowser stop];
}

-(void) updateSearchLabel
{
    static short count = 1;
    NSString * searchText = NSLocalizedString(@"Searching", nil);
    for (uint i = 0; i<=count; ++i) {
        searchText = [searchText stringByAppendingString:@"."];
    }
    NSLog(@"Updating count = %u SearchText = %@", count, searchText);
    self.searchStateText = searchText;
    count++;
    count = count % 3;
}

-(void) netServiceBrowserWillSearch:(NSNetServiceBrowser *)aNetServiceBrowser
{
    NSLog(@"Will search");
    self.comManager.searchState = SEARCHING;
    self.searchStateText = NSLocalizedString(@"Searching", nil);
    [self.searchLabelTimer invalidate];
    [self.searchTimeoutTimer invalidate];
    self.searchLabelTimer = [NSTimer scheduledTimerWithTimeInterval:1.0
                                                           target:self
                                                         selector:@selector(updateSearchLabel)
                                                         userInfo:nil
                                                          repeats:YES];
    
    self.searchTimeoutTimer = [NSTimer scheduledTimerWithTimeInterval:5.0
                                                                   target:self
                                                                 selector:@selector(handleSearchTimeout)
                                                                 userInfo:nil
                                                                  repeats:NO];
    
    [[NSRunLoop currentRunLoop] addTimer:self.searchLabelTimer forMode:NSRunLoopCommonModes];
    self.style = UITableViewCellSelectionStyleNone;
}

-(void) netServiceBrowserDidStopSearch:(NSNetServiceBrowser *)aNetServiceBrowser
{
    NSLog(@"End search");
    self.comManager.searchState = WAITING;
    [self.searchLabelTimer invalidate];
    [self.searchTimeoutTimer invalidate];
    self.searchStateText = NSLocalizedString(@"Click to refresh", nil);
    self.style = UITableViewCellSelectionStyleBlue;
}

- (void)netServiceBrowser:(NSNetServiceBrowser *)aNetServiceBrowser didNotSearch:(NSDictionary *)errorDict
{
    NSLog(@"search error");
//    [self.serviceBrowser searchForServicesOfType:@"_impressRemote._tcp" inDomain:@"local"];
}

- (void)netServiceBrowser:(NSNetServiceBrowser *)aNetServiceBrowser
           didFindService:(NSNetService *)aNetService
               moreComing:(BOOL)moreComing
{
    [self.comManager.autoDiscoveryServers removeObject:aNetService];
    [self.comManager.autoDiscoveryServers addObject:aNetService];
    
    NSLog(@"Got service %p with hostname %@\n", aNetService,
          [aNetService name]);
    
    [aNetService setDelegate:self];
    
    if(!moreComing)
    {
        [self.tableView reloadData];
    }
}

// Sent when a service disappears
- (void)netServiceBrowser:(NSNetServiceBrowser *)browser
         didRemoveService:(NSNetService *)aNetService
               moreComing:(BOOL)moreComing
{
    [self.comManager.autoDiscoveryServers removeObject:aNetService];
    
    if(!moreComing)
    {
        [self.tableView reloadData];
        if ([self.comManager.autoDiscoveryServers count] == 0) {
            UITableViewCell * cell = [self.tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
            [(UIActivityIndicatorView *)[cell viewWithTag:5] startAnimating];
        }
    }
}

# pragma mark - System defaults

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
{
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad)
        return YES;
    else {
        return toInterfaceOrientation == UIInterfaceOrientationMaskPortrait;
    }
}


- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
 
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
    
    
    self.center = [NSNotificationCenter defaultCenter];
    self.comManager = [CommunicationManager sharedComManager];
    self.serverTable.dataSource = self;
    self.serverTable.delegate = self;
    _style = UITableViewCellSelectionStyleNone;
    
    [self setTitle:NSLocalizedString(@"Impress Remote", @"App name displayed on navbar")];
    
}

- (void) viewDidAppear:(BOOL)animated
{
    NSOperationQueue *mainQueue = [NSOperationQueue mainQueue];
    self.pinValidationObserver = [[NSNotificationCenter defaultCenter] addObserverForName:STATUS_PAIRING_PINVALIDATION
                                                                                   object:nil
                                                                                    queue:mainQueue
                                                                               usingBlock:^(NSNotification *note) {
                                                                                   [self.comManager.client stopConnectionTimeoutTimer];
                                                                                   self.comManager.state = CONNECTED;
                                                                                   [self disableSpinner];
                                                                                   [self performSegueWithIdentifier:@"pinValidation" sender:self ];
                                                                               }];
    self.slideShowPreviewStartObserver = [[NSNotificationCenter defaultCenter] addObserverForName:STATUS_PAIRING_PAIRED
                                                                                           object:nil
                                                                                            queue:mainQueue
                                                                                       usingBlock:^(NSNotification *note) {
                                                                                           [self.comManager.client stopConnectionTimeoutTimer];
                                                                                           self.comManager.state = CONNECTED;
                                                                                           [self disableSpinner];
                                                                                           [self performSegueWithIdentifier:@"SlideShowPreview" sender:self ];
                                                                                       }];
    NSLog(@"Clear auto discovered servers");
    [self.comManager.autoDiscoveryServers removeAllObjects];
    [self.serverTable reloadData];
    self.serviceBrowser = [[NSNetServiceBrowser alloc] init];
    [self.serviceBrowser setDelegate:self];
    [self.serviceBrowser searchForServicesOfType:@"_impressremote._tcp" inDomain:@"local"];
    [self.serviceBrowser scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    [super viewDidAppear:animated];
}

- (void) viewWillDisappear:(BOOL)animated
{
    [self disableSpinner];
    [self.serviceBrowser stop];
    [self.serviceBrowser removeFromRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
    [super viewWillDisappear:animated];
}

- (void) viewDidDisappear:(BOOL)animated
{
    [[NSNotificationCenter defaultCenter] removeObserver:self.slideShowPreviewStartObserver];
    [[NSNotificationCenter defaultCenter] removeObserver:self.pinValidationObserver];
    [super viewDidDisappear:animated];
}


-(void)viewWillAppear:(BOOL)animated
{
    [self.comManager setDelegate:self];
    [self.serverTable reloadData];
    [super viewWillAppear:animated];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view delegate

- (void)disableSpinner
{
    @synchronized(self.lastSpinningCellIndex){
        if ([[self.tableView cellForRowAtIndexPath:self.lastSpinningCellIndex] respondsToSelector:@selector(accessoryView)]) {
            if (self.tableView && [self.tableView cellForRowAtIndexPath:self.lastSpinningCellIndex]) {
                [[self.tableView cellForRowAtIndexPath:self.lastSpinningCellIndex] setAccessoryView:nil];
            }
        }
    }
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    
    if ([self.comManager.autoDiscoveryServers count] == 0 && indexPath.section == 0){
        // No discovered server and not searching => in a "click to refresh" state, so we restart searching process
        if (self.comManager.searchState == WAITING){
            [self.serviceBrowser searchForServicesOfType:@"_impressremote._tcp" inDomain:@"local"];
            [self.serviceBrowser scheduleInRunLoop:[NSRunLoop currentRunLoop] forMode:NSDefaultRunLoopMode];
        }
        // Return when browser is still searching...
        return;
    }
    
    // Return when nothing should be done
    if (self.comManager.state == CONNECTING)
        return;
    
    UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
    
    // Setting up the spinner to the right cell
    self.lastSpinningCellIndex = indexPath;
    UIActivityIndicatorView *activityView = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
    [activityView startAnimating];
    [cell setAccessoryView:activityView];

    if (indexPath.section == 1){
        NSLog(@"Connecting to %@:%@", [[self.comManager.servers objectAtIndex:indexPath.row] serverName], [[self.comManager.servers objectAtIndex:indexPath.row] serverAddress]);
        [self.comManager connectToServer:[self.comManager.servers objectAtIndex:indexPath.row]];
    } else if (indexPath.section == 0){
        NSLog(@"Connecting to %@", [[self.comManager.autoDiscoveryServers objectAtIndex:indexPath.row] name]);
        [[self.comManager.autoDiscoveryServers objectAtIndex:indexPath.row] resolveWithTimeout:0.0];
    }
}

- (void)viewDidUnload {
    [self setServerTable:nil];
    [super viewDidUnload];
}

- (UIView *)tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section {
    
    NSString *sectionName = nil;
    
    switch (section) {
        case 0:
            sectionName = [NSString stringWithFormat:NSLocalizedString(@"Detected Connections", @"Server list section title")];
            break;
        case 1:
            sectionName = [NSString stringWithFormat:NSLocalizedString(@"Custom Connections", @"Server list section title")];
            break;
    }
    UILabel *sectionHeader;
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
        sectionHeader = [[UILabel alloc] initWithFrame:CGRectMake(10, 10, 310, 40)];
    } else {
        sectionHeader = [[UILabel alloc] initWithFrame:CGRectMake(25, 10, 295, 40)];
    }
    sectionHeader.backgroundColor = [UIColor clearColor];
    sectionHeader.font = kAppTextFont;
    sectionHeader.textColor = [UIColor darkTextColor];
    sectionHeader.text = sectionName;
    UIView *view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, tableView.bounds.size.width, [self tableView:tableView heightForHeaderInSection:section])];
    [view addSubview:sectionHeader];
    
    return view;
}

- (UIView *)tableView:(UITableView *)tableView viewForFooterInSection:(NSInteger)section {
    if ([self.comManager.servers count] == 0 && section == 1) {
        UILabel *sectionFooter = [[UILabel alloc] initWithFrame:CGRectMake(20, 10, tableView.frame.size.width - 50, 60)];
        [sectionFooter setLineBreakMode:NSLineBreakByWordWrapping];
        [sectionFooter setNumberOfLines:5];
        sectionFooter.backgroundColor = [UIColor clearColor];
        sectionFooter.font = kAppSmallTextFont;
        sectionFooter.textColor = kTintColor;
        sectionFooter.text = NSLocalizedString(@"Customize server config instruction", @"Displayed when no customized server is available");
        
        UIView *view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, tableView.bounds.size.width, [self tableView:tableView heightForHeaderInSection:section])];
        [view addSubview:sectionFooter];
        return view;
    }
    return nil;
}

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section
{
    return 50.0;
}

-(NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    return 2;
}

-(NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    switch (section) {
        case 0:
            return [self.comManager.autoDiscoveryServers count] == 0 ? 1 : [self.comManager.autoDiscoveryServers count];
            break;
        case 1:
            return [self.comManager.servers count];
        default:
            return -1;
            break;
    }
}

-(UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    static NSString *cellIdentifier = @"server_item_cell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellIdentifier];
    
    if (indexPath.section == 0){
        if ([self.comManager.autoDiscoveryServers count] == 0){
            cell.textLabel.text = NSLocalizedString(self.searchStateText, nil);
            cell.textLabel.lineBreakMode = UILineBreakModeClip;
            cell.selectionStyle = self.style;
        } else {
            id s = [self.comManager.autoDiscoveryServers objectAtIndex:indexPath.row];
            
            if ([s isKindOfClass:[Server class]]) {
                [cell.textLabel setText:[s serverName]];
                [cell.detailTextLabel setText:[s serverAddress]];
            } else if ([s isKindOfClass:[NSNetService class]]){
                [cell.textLabel setText:[s name]];
                [cell.detailTextLabel setText:@""];
            }
        }
    }
    else {
        Server *s = [self.comManager.servers objectAtIndex:indexPath.row];
        
        [cell.textLabel setText:[s serverName]];
        [cell.detailTextLabel setText:[s serverAddress]];
    }
    return cell;
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.section == 1)
        return YES;
    else
        return NO;
}

- (UITableViewCellEditingStyle)tableView:(UITableView *)tableView editingStyleForRowAtIndexPath:(NSIndexPath *)indexPath{
    if (indexPath.section == 1) {
        return UITableViewCellEditingStyleDelete;
    } else {
        return UITableViewCellEditingStyleNone;
    }
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath{
    
    if(editingStyle == UITableViewCellEditingStyleDelete){
        [self.comManager removeServerAtIndex:indexPath.row];
        [tableView deleteRowsAtIndexPaths:[NSArray arrayWithObject:indexPath] withRowAnimation:UITableViewRowAnimationFade];
    }
}

@end
