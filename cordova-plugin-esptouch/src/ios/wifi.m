#import "wifi.h"
#import <CoreLocation/CoreLocation.h>;

@interface wifi() <CLLocationManagerDelegate>;
@property (nonatomic, strong) CLLocationManager *locationManager;
@property (nonatomic, strong) CDVInvokedUrlCommand *command;
@end
@implementation wifi


- (void) getConnectedInfo:(CDVInvokedUrlCommand *)command{
    self.command = command;
    
    [self isLocationEnabled];
}



- (void)isLocationEnabled {
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
    [self.locationManager requestAlwaysAuthorization];
}

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status {
    NSLog(@"Authorization status: %d",status);
    if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
        if (@available(iOS 14.0, *)) {
            if (manager.accuracyAuthorization == CLAccuracyAuthorizationReducedAccuracy) {
                [manager requestTemporaryFullAccuracyAuthorizationWithPurposeKey:@"getSSID"];
            } else {
                [self getWiFiInfo];
            }
        } else {
            [self getWiFiInfo];
        }
    }
}

- (void)getWiFiInfo {
    NSLog(@"getConnectedInfo wifiDic begin");
    NSDictionary *wifiDic = @{
        @"ssid":ESPTools.getCurrentWiFiSsid,
        @"bssid":ESPTools.getCurrentBSSID,
        @"state":@"Connected"
    };
    NSLog(@"getConnectedInfo wifiDic end");

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:wifiDic];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.command.callbackId];
}

@end