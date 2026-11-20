import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'screens/blackbox_home_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Color(0xFF000000),
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );
  runApp(const BlackBoxApp());
}

class BlackBoxApp extends StatelessWidget {
  const BlackBoxApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BlackBox',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF000000),
        primaryColor: const Color(0xFF00FF66),
        fontFamily: 'monospace',
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF00FF66),
          secondary: Color(0xFF00E5FF),
          surface: Color(0xFF0D1117),
        ),
      ),
      home: const BlackBoxHomeScreen(),
    );
  }
}
