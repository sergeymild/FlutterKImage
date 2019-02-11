import Flutter
import UIKit
import Kingfisher
import AVFoundation

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
    
    
    
    guard let dict = call.arguments as? [String: Any] else {
        result("dict is empty")
        return
    }
    
    let width = dict["width"] as? Int
    let height = dict["height"] as? Int
    let quality = dict["quality"] as? Int
    
    let path = dict["path"] as! String
    
    let absolutePath = URL(fileURLWithPath: path)
    var options: KingfisherOptionsInfo = [.backgroundDecode, .scaleFactor(0.8)]
    if let width = width, let height = height {
        let resizeProcessor = ResizingImageProcessor(referenceSize: CGSize(width: width, height: height), mode: .aspectFill)
        options.append(.processor(resizeProcessor))
    }
    
    if let pending = tasks.removeValue(forKey: path) {
        pending.cancel()
    }
    
    var provider: ImageDataProvider?
    if call.method == "fetchArtworkFromLocalPath" {
        provider = Mp3ImageProvider(fileURL: absolutePath)
    } else if call.method == "loadImageFromLocalPath" {
        provider = LocalFileImageDataProvider(fileURL: absolutePath)
    } else if call.method == "fetchVideoThumbnailFromLocalPath" {
        provider = VideoThumbnailImageProvider(fileURL: absolutePath)
    }
    
    guard let imageProvider = provider else {
        result("provider is not found")
        return
    }
    
    let task = KingfisherManager.shared.retrieveImage(with: .provider(imageProvider), options: options, progressBlock: nil, completionHandler: { (res) in
        switch res {
        case .success(let data):
            self.queue.async {
                let image = data.image
                let i = UIImage(cgImage: data.image.cgImage!)
                let jpeg = UIImagePNGRepresentation(image)
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
