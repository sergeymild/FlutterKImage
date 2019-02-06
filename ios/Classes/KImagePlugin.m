#import "KImagePlugin.h"
#import <k_image/k_image-Swift.h>

@implementation KImagePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftKImagePlugin registerWithRegistrar:registrar];
}
@end
