import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:k_image/k_image.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: FutureBuilder<MemoryImage>(
              future: KImage.loadFromLocalPath(
                  path: "/storage/emulated/0/AlohaUserDownloads/Avicii - Wake Me Up (Official Video).mp3",
                  width: 300,
                  height: 300,
                  quality: 100,
                  isMp3Artwork: true),
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return Text("load");
                }

                return Image(
                  image: snapshot.data,
                  width: 200,
                  height: 200,
                );
              }),
        ),
      ),
    );
  }
}
