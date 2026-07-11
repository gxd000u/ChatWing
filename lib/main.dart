import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'app.dart';
import 'providers/engine_state.dart';
import 'providers/contact_state.dart';
import 'providers/profile_state.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => EngineState()),
        ChangeNotifierProvider(create: (_) => ContactState()),
        ChangeNotifierProvider(create: (_) => ProfileState()),
      ],
      child: const ChatWingApp(),
    ),
  );
}
