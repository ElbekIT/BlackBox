import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/**
 * BlackBox — №1 Zero-Delay Cyber Privacy, Anti-Theft & AppLock Suite.
 * Brend: Elbek Web (@elbekdesign_va_webdasturchi_uz)
 * 100% Oflayn, Zero Internet, Hardware Keystore & 0 ms Native Security.
 */
class BlackBoxSecurityScreen extends StatefulWidget {
  const BlackBoxSecurityScreen({super.key});

  @override
  State<BlackBoxSecurityScreen> createState() => _BlackBoxSecurityScreenState();
}

class _BlackBoxSecurityScreenState extends State<BlackBoxSecurityScreen> {
  static const MethodChannel _platform = MethodChannel('uz.elbekweb.blackbox/security');

  List<dynamic> _permissions = [];
  bool _isLoading = true;
  int _securityScore = 0;
  bool _calculatorDisguise = false;

  @override
  void initState() {
    super.initState();
    _loadSecurityStatus();
  }

  Future<void> _loadSecurityStatus() async {
    try {
      final List<dynamic> permissions = await _platform.invokeMethod('checkPermissions');
      int grantedCount = permissions.where((p) => p['isGranted'] == true).length;
      int score = permissions.isNotEmpty ? ((grantedCount * 100) / permissions.length).round() : 0;

      setState(() {
        _permissions = permissions;
        _securityScore = score;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _requestPermission(String type) async {
    try {
      await _platform.invokeMethod('requestPermission', {'type': type});
      await Future.delayed(const Duration(seconds: 1));
      _loadSecurityStatus();
    } catch (e) {
      // Ignored
    }
  }

  Future<void> _toggleDisguise() async {
    try {
      final bool newState = await _platform.invokeMethod('toggleCalculatorDisguise');
      setState(() {
        _calculatorDisguise = newState;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            newState
                ? 'Kalkulyator niqobi faollashtirildi!'
                : 'BlackBox asl ko\'rinishiga qaytarildi!',
            style: const TextStyle(fontFamily: 'monospace', color: Color(0xFF00FF66)),
          ),
          backgroundColor: const Color(0xFF0D1117),
        ),
      );
    } catch (e) {
      // Ignored
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF000000), // Pure AMOLED Black
      appBar: AppBar(
        backgroundColor: const Color(0xFF000000),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: Color(0xFF00FF66)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'BLACKBOX // CYBER VAULT',
          style: TextStyle(
            color: Color(0xFF00FF66),
            fontFamily: 'monospace',
            fontWeight: FontWeight.bold,
            fontSize: 16,
            letterSpacing: 1.5,
          ),
        ),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF00FF66)))
          : RefreshIndicator(
              color: const Color(0xFF00FF66),
              backgroundColor: const Color(0xFF0D1117),
              onRefresh: _loadSecurityStatus,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 580),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Subtitle
                        const Text(
                          'Elbek Web • 100% Oflayn Kiber Maxfiylik',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: Color(0xFF00E5FF),
                            fontFamily: 'monospace',
                            fontSize: 12,
                          ),
                        ),
                        const SizedBox(height: 24),

                        // Cyber Guardian Score Card
                        Container(
                          padding: const EdgeInsets.all(24),
                          decoration: BoxDecoration(
                            color: const Color(0xFF0D1117),
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(
                              color: _securityScore > 75
                                  ? const Color(0xFF00FF66)
                                  : (_securityScore > 40
                                      ? const Color(0xFF00E5FF)
                                      : const Color(0xFFFF0055)),
                              width: 2,
                            ),
                          ),
                          child: Column(
                            children: [
                              const Text(
                                'CYBER GUARDIAN XAVFSIZLIK REYTINGI',
                                style: TextStyle(
                                  color: Color(0xFF8B949E),
                                  fontFamily: 'monospace',
                                  fontSize: 11,
                                  letterSpacing: 1,
                                ),
                              ),
                              const SizedBox(height: 12),
                              Text(
                                '$_securityScore / 100',
                                style: TextStyle(
                                  color: _securityScore > 75
                                      ? const Color(0xFF00FF66)
                                      : (_securityScore > 40
                                          ? const Color(0xFF00E5FF)
                                          : const Color(0xFFFF0055)),
                                  fontFamily: 'monospace',
                                  fontSize: 42,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                _securityScore > 75
                                    ? 'MAKSIMAL HIMOYALANGAN'
                                    : 'XAVFSIZLIK DARAJASI PAST',
                                style: TextStyle(
                                  color: _securityScore > 75
                                      ? const Color(0xFF00FF66)
                                      : const Color(0xFFFF0055),
                                  fontFamily: 'monospace',
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 32),

                        // Section Title
                        const Text(
                          'TIZIM RUXSATLARI WIZARD (0 ms Himoya)',
                          style: TextStyle(
                            color: Color(0xFFE6EDF3),
                            fontFamily: 'monospace',
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 14),

                        // Permission list
                        ..._permissions.map((p) => _buildPermissionCard(p)),

                        const SizedBox(height: 28),

                        // Stealth Disguise Button
                        ElevatedButton.icon(
                          onPressed: _toggleDisguise,
                          icon: const Icon(Icons.shield_outlined, color: Colors.black),
                          label: Text(
                            _calculatorDisguise
                                ? 'KALKULYATOR NIQOBI: YONIQ'
                                : 'KALKULYATOR NIQOBINI YOQISH',
                            style: const TextStyle(
                              color: Colors.black,
                              fontFamily: 'monospace',
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF00E5FF),
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(14),
                            ),
                          ),
                        ),
                        const SizedBox(height: 32),
                      ],
                    ),
                  ),
                ),
              ),
            ),
    );
  }

  Widget _buildPermissionCard(dynamic p) {
    final bool isGranted = p['isGranted'] == true;
    final String title = p['title'] ?? '';
    final String type = p['type'] ?? '';

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: const Color(0xFF161B22),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: isGranted ? const Color(0xFF00FF66) : const Color(0xFF30363D),
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Icon(
            isGranted ? Icons.check_circle : Icons.warning_amber_rounded,
            color: isGranted ? const Color(0xFF00FF66) : const Color(0xFFFF0055),
            size: 24,
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Text(
              title,
              style: TextStyle(
                color: isGranted ? const Color(0xFF00FF66) : const Color(0xFFE6EDF3),
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          const SizedBox(width: 10),
          GestureDetector(
            onTap: isGranted ? null : () => _requestPermission(type),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: isGranted ? const Color(0xFF002811) : const Color(0xFF00FF66),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFF00FF66)),
              ),
              child: Text(
                isGranted ? 'AKTIV' : 'BERISH',
                style: TextStyle(
                  color: isGranted ? const Color(0xFF00FF66) : Colors.black,
                  fontFamily: 'monospace',
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
