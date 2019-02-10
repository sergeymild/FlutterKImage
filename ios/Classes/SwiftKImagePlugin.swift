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
    
    if call.method == "fetchArtworkFromLocalPath" {
        
        queue.async {
            let playerItem = AVPlayerItem(url: URL.init(fileURLWithPath: path))
            let metadataList = playerItem.asset.metadata
            var isFound = false
            for item in metadataList {
                if item.commonKey?.rawValue  == "artwork" {
                    guard let data = item.value as? Data else {
                        result(nil)
                        return
                    }
                    
                    var image = data
                    if let width = width, let height = height, let img = UIImage(data: data) {
                        if let resized = self.resizeImage(image: img, targetSize: CGSize(width: CGFloat(width), height: CGFloat(height))) {
                            image = resized
                        }
                    }
                    
                    result(image)
                    isFound = true
                    break
                }
            }
            
            if !isFound {
                result(nil)
            }
        }
        
        return
    }
    
    if call.method == "loadImageFromLocalPath" {
        let absolutePath = URL(fileURLWithPath: path)
        let provider = LocalFileImageDataProvider(fileURL: absolutePath)
        
        var options: KingfisherOptionsInfo = [.backgroundDecode]
        if let width = width, let height = height {
            let cropProcessor = CroppingImageProcessor(size: CGSize(width: width, height: height))
            options.append(.processor(cropProcessor))
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
    
    func resizeImage(image: UIImage, targetSize: CGSize) -> Data? {
        let size = image.size
        
        let widthRatio  = targetSize.width  / image.size.width
        let heightRatio = targetSize.height / image.size.height
        
        // Figure out what our orientation is, and use that to form the rectangle
        var newSize: CGSize
        if(widthRatio > heightRatio) {
            newSize = CGSize(width: size.width * heightRatio, height: size.height * heightRatio)
        } else {
            newSize = CGSize(width: size.width * widthRatio,  height: size.height * widthRatio)
        }
        
        // This is the rect that we've calculated out and this is what is actually used below
        let rect = CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height)
        
        // Actually do the resizing to the rect using the ImageContext stuff
        UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
        image.draw(in: rect)
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        guard let img = newImage, let data = UIImagePNGRepresentation(img) else {
            return nil
        }
        
        return data
    }
}
