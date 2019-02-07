import Flutter
import UIKit
import Kingfisher

public class SwiftKImagePlugin: NSObject, FlutterPlugin {
    
    let queue = DispatchQueue(label: "SwiftKImagePlugin")
    var tasks: [String: DownloadTask] = [:]
    
    lazy var documentsFolder: String = {
        return NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
    }()
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "k_image", binaryMessenger: registrar.messenger())
    let instance = SwiftKImagePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if call.method == "documentsFolder" {
        result(documentsFolder)
        return
    }
    if call.method == "loadImageFromLocalPath" {
        if let dict = call.arguments as? [String: Any] {
            let width = dict["width"] as? Int
            let height = dict["height"] as? Int
            let quality = dict["quality"] as? Int
            
            let path = dict["path"] as! String
            
            let absolutePath = URL(fileURLWithPath: path)
            let provider = LocalFileImageDataProvider(fileURL: absolutePath)
            
            var options: KingfisherOptionsInfo = [.backgroundDecode]
            if let width = width, let height = height {
                options.append(.processor(ResizingImageProcessor(referenceSize: CGSize(width: width, height: height))))
            }
            
            if let pending = tasks.removeValue(forKey: absolutePath.absoluteString) {
                pending.cancel()
            }
            
            
            let task = KingfisherManager.shared.retrieveImage(with: .provider(provider), options: options, progressBlock: nil, completionHandler: { (res) in
                switch res {
                case .success(let data):
                    self.queue.async {
                        let jpeg = UIImagePNGRepresentation(data.image)
                        result(jpeg)
                        
                        if let pending = self.tasks.removeValue(forKey: absolutePath.absoluteString) {
                            pending.cancel()
                        }
                    }
                    break;
                case .failure(let error):
                    debugPrint(error)
                    break
                }
            })
            
            if let task = task {
                tasks[absolutePath.absoluteString] = task
            }
        }
    }
  }
}
