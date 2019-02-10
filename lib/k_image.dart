import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class KImage {
  static const MethodChannel _channel = const MethodChannel('k_image');

  static Future<MemoryImage> loadFromLocalPath(
      {String path,
      int width,
      int height,
      int quality,
      bool skipFetchDocumentsFolder = true,
      bool isMp3Artwork = false,
      bool isVideoArtwork = false,
      bool debug = false}) async {
    String documentsFolder = "";
    if (!skipFetchDocumentsFolder) {
      documentsFolder = await _channel.invokeMethod("documentsFolder");
      documentsFolder += "/";
    }
    final params = {
      "path": "$documentsFolder$path",
      "width": width,
      "height": height,
      "quality": quality
    };
    if (debug) {
      if (!skipFetchDocumentsFolder) print('documentsFolder: $documentsFolder');
      print('loadImageWithParams: $params');
    }
    
    String callMethod = "loadImageFromLocalPath";
    if (isMp3Artwork) callMethod = "fetchArtworkFromLocalPath";
    if (isVideoArtwork) callMethod = "fetchVideoThumbnailFromLocalPath";

    final List<int> bytes =
        await _channel.invokeMethod(callMethod, params);
    final image = MemoryImage(bytes);
    return image;
  }
}
