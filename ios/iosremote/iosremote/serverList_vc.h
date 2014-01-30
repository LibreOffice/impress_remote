// -*- Mode: ObjC; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
//
// This file is part of the LibreOffice project.
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

#import <UIKit/UIKit.h>

@interface server_list_vc : UITableViewController <UITableViewDelegate, UITableViewDataSource>

- (void)disableSpinner;

@property (strong, nonatomic) IBOutlet UITableView *serverTable;

@end
