//
//  Mp3ImageProvider.swift
//  k_image
//
//  Created by Sergei Golishnikov on 10/02/2019.
//

import Foundation
import Kingfisher
import AVFoundation


/// Represents an image data provider for loading from a local file URL on disk.
/// Uses this type for adding a disk image to Kingfisher. Compared to loading it
/// directly, you can get benefit of using Kingfisher's extension methods, as well
/// as applying `ImageProcessor`s and storing the image to `ImageCache` of Kingfisher.
public struct Mp3ImageProvider: ImageDataProvider {
    
    // MARK: Public Properties
    
    /// The file URL from which the image be loaded.
    public let fileURL: URL
    
    // MARK: Initializers
    
    /// Creates an image data provider by supplying the target local file URL.
    ///
    /// - Parameters:
    ///   - fileURL: The file URL from which the image be loaded.
    ///   - cacheKey: The key is used for caching the image data. By default,
    ///               the `absoluteString` of `fileURL` is used.
    public init(fileURL: URL, cacheKey: String? = nil) {
        self.fileURL = fileURL
        self.cacheKey = cacheKey ?? fileURL.absoluteString
    }
    
    // MARK: Protocol Conforming
    
    /// The key used in cache.
    public var cacheKey: String
    
    public func data(handler: (Result<Data, Error>) -> Void) {
        let playerItem = AVPlayerItem(url: fileURL)
        let metadataList = playerItem.asset.metadata
        for item in metadataList {
            if item.commonKey?.rawValue  == "artwork" {
                guard let data = item.value as? Data else {
                    return
                }
                handler(Result.success(data))
                return
            }
        }
        handler(Result.success(Data(count: 0)))
    }
}
