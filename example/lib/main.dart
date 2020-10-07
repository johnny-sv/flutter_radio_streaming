import 'package:flutter/material.dart';
import 'StreamingController.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  var streamingController = StreamingController();

  @override
  void initState() {
    super.initState();
    streamingController.config(
        url: "http://riverfm.com.au/listen.m3u");
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              FlatButton(
                onPressed: () {
                  streamingController.play();
                },
                child: Text(
                  "Play",
                ),
              ),
              FlatButton(
                onPressed: () {
                  streamingController.pause();
                },
                child: Text(
                  "Pause",
                ),
              ),
              FlatButton(
                onPressed: () {
                  streamingController.stop();
                },
                child: Text(
                  "Stop",
                ),
              ),
              StreamBuilder(
                stream: streamingController.streamingController.stream,
                builder: (BuildContext context, snapshot) {
                  if (snapshot.hasData) {
                    return new Text(
                      snapshot.data,
                      style: Theme.of(context).textTheme.headline4,
                    );
                  } else {
                    return new Text("", style: Theme.of(context).textTheme.headline4,
                    );
                  }
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
