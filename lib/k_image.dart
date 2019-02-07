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
      bool debug = true}) async {
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

    final List<int> bytes =
        await _channel.invokeMethod('loadImageFromLocalPath', params);
    final image = MemoryImage(bytes);
    return image;
  }
}
