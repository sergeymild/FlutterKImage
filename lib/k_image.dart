import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class KImage {
  static const MethodChannel _channel = const MethodChannel('k_image');

  static Future<MemoryImage> loadFromLocalPath(
      {String path, int width, int height, int quality}) async {
    final List<int> bytes = await _channel.invokeMethod(
        'loadImageFromLocalPath',
        {"path": path, "width": width, "height": height, "quality": quality});
    final image = MemoryImage(bytes);
    return image;
  }
}
