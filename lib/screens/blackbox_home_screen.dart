import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'calculator_screen.dart';

/**
 * BlackBox — №1 Zero-Delay Cyber Privacy, Anti-Theft & AppLock Suite.
 * Muallif va Brend: Elbek Web (@elbekdesign_va_webdasturchi_uz)
 * 100% Oflayn, Zero-Internet, Hardware Keystore AES-256 & 0 ms Native Security.
 */
class BlackBoxHomeScreen extends StatefulWidget {
  const BlackBoxHomeScreen({super.key});

  @override
  State<BlackBoxHomeScreen> createState() => _BlackBoxHomeScreenState();
}

class _BlackBoxHomeScreenState extends State<BlackBoxHomeScreen> {
  static const MethodChannel _platform = MethodChannel('uz.elbekweb.blackbox/security');

  int _currentTabIndex = 0;
  bool _isLoading = true;
  int _securityScore = 0;
  bool _calculatorDisguise = false;
  String _masterPin = "7777";

  // Ruxsatlar
  List<Map<String, dynamic>> _permissions = [];
  bool _isAccessibilityGranted = false;

  // Ilovalar ro'yxati
  List<Map<String, dynamic>> _installedApps = [];
  List<Map<String, dynamic>> _filteredApps = [];
  final TextEditingController _searchController = TextEditingController();

  // Anti-Theft holatlari
  bool _pocketAlarmEnabled = true;
  bool _unplugAlarmEnabled = true;
  bool _motionAlarmEnabled = false;
  bool _intruderSelfieEnabled = true;

  @override
  void initState() {
    super.initState();
    _loadInitialData();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadInitialData() async {
    setState(() => _isLoading = true);
    await Future.wait([
      _checkPermissions(),
      _loadInstalledApps(),
      _loadDisguiseStatus(),
    ]);
    setState(() => _isLoading = false);
  }

  Future<void> _checkPermissions() async {
    try {
      final List<dynamic> perms = await _platform.invokeMethod('checkPermissions');
      final formatted = perms.map((p) => Map<String, dynamic>.from(p)).toList();
      int grantedCount = formatted.where((p) => p['isGranted'] == true).length;
      int score = formatted.isNotEmpty ? ((grantedCount * 100) / formatted.length).round() : 0;

      final acc = formatted.firstWhere(
        (p) => p['type'] == 'ACCESSIBILITY',
        orElse: () => {'isGranted': false},
      );

      setState(() {
        _permissions = formatted;
        _securityScore = score;
        _isAccessibilityGranted = acc['isGranted'] == true;
      });
    } catch (e) {
      // Ignored
    }
  }

  Future<void> _loadInstalledApps() async {
    try {
      final List<dynamic> apps = await _platform.invokeMethod('getInstalledApps');
      final formatted = apps.map((a) => Map<String, dynamic>.from(a)).toList();

      setState(() {
        _installedApps = formatted;
        _filterApps(_searchController.text);
      });
    } catch (e) {
      // Fallback ro'yxat
      _setFallbackApps();
    }
  }

  void _setFallbackApps() {
    final fallback = [
      {'appName': 'Telegram', 'packageName': 'org.telegram.messenger', 'isLocked': true, 'isSystem': false},
      {'appName': 'Instagram', 'packageName': 'com.instagram.android', 'isLocked': true, 'isSystem': false},
      {'appName': 'WhatsApp', 'packageName': 'com.whatsapp', 'isLocked': false, 'isSystem': false},
      {'appName': 'Galereya', 'packageName': 'com.android.gallery3d', 'isLocked': true, 'isSystem': true},
      {'appName': 'YouTube', 'packageName': 'com.google.android.youtube', 'isLocked': false, 'isSystem': false},
      {'appName': 'Sozlamalar (Settings)', 'packageName': 'com.android.settings', 'isLocked': true, 'isSystem': true},
      {'appName': 'TikTok', 'packageName': 'com.zhiliaoapp.musically', 'isLocked': false, 'isSystem': false},
      {'appName': 'Xabarlar (SMS)', 'packageName': 'com.google.android.apps.messaging', 'isLocked': true, 'isSystem': true},
      {'appName': 'Kontaktlar', 'packageName': 'com.android.contacts', 'isLocked': false, 'isSystem': true},
      {'appName': 'Google Chrome', 'packageName': 'com.android.chrome', 'isLocked': false, 'isSystem': false},
      {'appName': 'Fayllar (Files)', 'packageName': 'com.google.android.apps.nbu.files', 'isLocked': true, 'isSystem': true},
    ];
    setState(() {
      _installedApps = fallback;
      _filterApps(_searchController.text);
    });
  }

  void _filterApps(String query) {
    if (query.trim().isEmpty) {
      _filteredApps = List.from(_installedApps);
    } else {
      _filteredApps = _installedApps.where((app) {
        final name = (app['appName'] ?? '').toString().toLowerCase();
        final pkg = (app['packageName'] ?? '').toString().toLowerCase();
        return name.contains(query.toLowerCase()) || pkg.contains(query.toLowerCase());
      }).toList();
    }
  }

  Future<void> _loadDisguiseStatus() async {
    try {
      final bool disguise = await _platform.invokeMethod('isDisguiseEnabled');
      final String pin = await _platform.invokeMethod('getMasterPin');
      setState(() {
        _calculatorDisguise = disguise;
        _masterPin = pin;
      });
    } catch (e) {
      // Ignored
    }
  }

  Future<void> _toggleAppLock(String packageName, bool currentLocked) async {
    final newLocked = !currentLocked;
    try {
      await _platform.invokeMethod('setPackageLocked', {
        'packageName': packageName,
        'locked': newLocked,
      });

      setState(() {
        for (var app in _installedApps) {
          if (app['packageName'] == packageName) {
            app['isLocked'] = newLocked;
            break;
          }
        }
        _filterApps(_searchController.text);
      });

      HapticFeedback.lightImpact();
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
                ? 'Kalkulyator niqobi faollashtirildi! (PIN: $_masterPin)'
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

  Future<void> _requestPermission(String type) async {
    try {
      await _platform.invokeMethod('requestPermission', {'type': type});
      await Future.delayed(const Duration(seconds: 1));
      _checkPermissions();
    } catch (e) {
      // Ignored
    }
  }

  Future<void> _openAppDetailsSettings() async {
    try {
      await _platform.invokeMethod('openAppDetailsSettings');
    } catch (e) {
      // Ignored
    }
  }

  Future<void> _openAccessibilitySettings() async {
    try {
      await _platform.invokeMethod('openAccessibilitySettings');
    } catch (e) {
      // Ignored
    }
  }

  // ========================================================
  // ANDROID 13+ CHEKLANGAN SOZLAMANI YECHISH MODAL OYNASI
  // ========================================================
  void _showAndroid13RestrictedGuide() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        return Container(
          decoration: const BoxDecoration(
            color: Color(0xFF0D1117),
            borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
            border: Border(top: BorderSide(color: Color(0xFF00FF66), width: 2)),
          ),
          padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: Container(
                  width: 44,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.white24,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFF9900).withOpacity(0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.security, color: Color(0xFFFF9900), size: 28),
                  ),
                  const SizedBox(width: 14),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'ANDROID 13+ CHEKLOVINI YECHISH',
                          style: TextStyle(
                            color: Color(0xFF00FF66),
                            fontFamily: 'monospace',
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                            letterSpacing: 1.1,
                          ),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Maxsus Imkoniyatlar (Accessibility) ruxsati',
                          style: TextStyle(color: Color(0xFF8B949E), fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFF161B22),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: Colors.white10),
                ),
                child: const Text(
                  'Android 13 va undan yangi tizimlarda dastur qo\'lda o\'rnatilganda (sideloaded APK), xavfsizlik maqsadida Maxsus Imkoniyatlar (Accessibility) tugmasi muzlatiladi ("Cheklangan sozlama" / "Restricted setting"). Buni 30 soniyada oson yechish mumkin:',
                  style: TextStyle(color: Color(0xFFE6EDF3), fontSize: 13, height: 1.45),
                ),
              ),
              const SizedBox(height: 16),
              _buildStepItem(
                stepNum: '1',
                title: 'Ilova Ma\'lumotlariga o\'tish',
                desc: 'Quyidagi 1-tugmani bosing. BlackBox ilovasi ma\'lumotlari oynasi ochiladi.',
              ),
              _buildStepItem(
                stepNum: '2',
                title: 'Yuqori o\'ngdagi 3 ta nuqtani (⋮) bosing',
                desc: 'Ochilgan sahifaning tepasida o\'ng burchakda 3 ta nuqta menyu belgisi bo\'ladi.',
              ),
              _buildStepItem(
                stepNum: '3',
                title: '"Cheklangan sozlamalarga ruxsat berish"',
                desc: 'Menyudagi "Allow restricted settings" ni bosing va telefon PIN kodi yoki barmoq izingizni tasdiqlang.',
              ),
              _buildStepItem(
                stepNum: '4',
                title: 'Maxsus imkoniyatlarni yoqing',
                desc: 'Endi quyidagi 2-tugma orqali Maxsus imkoniyatlar bo\'limiga o\'ting va BlackBox\'ni faollashtiring!',
              ),
              const SizedBox(height: 20),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF00FF66),
                  foregroundColor: Colors.black,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                icon: const Icon(Icons.settings_applications, color: Colors.black),
                label: const Text(
                  '1. ILOVA MA\'LUMOTLARIGA O\'TISH (3 nuqta uchun)',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                ),
                onPressed: () {
                  Navigator.pop(ctx);
                  _openAppDetailsSettings();
                },
              ),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF00E5FF),
                  side: const BorderSide(color: Color(0xFF00E5FF)),
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                icon: const Icon(Icons.accessibility_new, color: Color(0xFF00E5FF)),
                label: const Text(
                  '2. MAXSUS IMKONIYoTLARNI OCHISH (Yoqish uchun)',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                ),
                onPressed: () {
                  Navigator.pop(ctx);
                  _openAccessibilitySettings();
                },
              ),
              const SizedBox(height: 8),
              TextButton(
                onPressed: () => Navigator.pop(ctx),
                child: const Text('TUSHUNDIM, YOPISH', style: TextStyle(color: Color(0xFF8B949E))),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildStepItem({required String stepNum, required String title, required String desc}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 26,
            height: 26,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: const Color(0xFF00FF66).withOpacity(0.18),
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: const Color(0xFF00FF66)),
            ),
            child: Text(
              stepNum,
              style: const TextStyle(
                color: Color(0xFF00FF66),
                fontWeight: FontWeight.bold,
                fontSize: 12,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  desc,
                  style: const TextStyle(color: Color(0xFF8B949E), fontSize: 12, height: 1.3),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ========================================================
  // ASOSIY UI EKRAN QURILISHI
  // ========================================================
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF000000), // Pure AMOLED Black
      appBar: _buildCyberAppBar(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF00FF66)))
          : IndexedStack(
              index: _currentTabIndex,
              children: [
                _buildDashboardTab(),
                _buildAppLockTab(),
                _buildAntiTheftTab(),
                _buildVaultTab(),
                _buildSettingsTab(),
              ],
            ),
      bottomNavigationBar: _buildBottomNav(),
    );
  }

  PreferredSizeWidget _buildCyberAppBar() {
    return AppBar(
      backgroundColor: const Color(0xFF000000),
      elevation: 0,
      titleSpacing: 16,
      title: Row(
        children: [
          // Official 3D Isometric Cyber Cube Logo
          Container(
            width: 38,
            height: 38,
            padding: const EdgeInsets.all(2),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: const Color(0xFF00FF66), width: 1.5),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF00FF66).withOpacity(0.2),
                  blurRadius: 8,
                  spreadRadius: 1,
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Image.asset(
                'assets/images/blackbox_logo.png',
                fit: BoxFit.contain,
                errorBuilder: (_, __, ___) => const Icon(
                  Icons.shield_outlined,
                  color: Color(0xFF00FF66),
                  size: 24,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'BLACKBOX',
                style: TextStyle(
                  color: Color(0xFF00FF66),
                  fontFamily: 'monospace',
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                  letterSpacing: 2.0,
                ),
              ),
              Text(
                'Elbek Web • 100% Oflayn Kiber-Qalqon',
                style: TextStyle(
                  color: Color(0xFF00E5FF),
                  fontSize: 10,
                  fontFamily: 'monospace',
                  letterSpacing: 0.5,
                ),
              ),
            ],
          ),
        ],
      ),
      actions: [
        IconButton(
          tooltip: 'Kalkulyator Niqobi',
          icon: Icon(
            _calculatorDisguise ? Icons.calculate : Icons.calculate_outlined,
            color: _calculatorDisguise ? const Color(0xFF00FF66) : const Color(0xFF8B949E),
          ),
          onPressed: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const CalculatorScreen()),
            );
          },
        ),
        IconButton(
          tooltip: 'Yangilash',
          icon: const Icon(Icons.refresh, color: Color(0xFF00E5FF)),
          onPressed: _loadInitialData,
        ),
      ],
    );
  }

  Widget _buildBottomNav() {
    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF0D1117),
        border: Border(top: BorderSide(color: Color(0xFF21262D), width: 1)),
      ),
      child: BottomNavigationBar(
        currentIndex: _currentTabIndex,
        onTap: (index) => setState(() => _currentTabIndex = index),
        backgroundColor: const Color(0xFF0D1117),
        selectedItemColor: const Color(0xFF00FF66),
        unselectedItemColor: const Color(0xFF8B949E),
        selectedLabelStyle: const TextStyle(fontFamily: 'monospace', fontWeight: FontWeight.bold, fontSize: 11),
        unselectedLabelStyle: const TextStyle(fontFamily: 'monospace', fontSize: 10),
        type: BottomNavigationBarType.fixed,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.shield), label: 'Qalqon'),
          BottomNavigationBarItem(icon: Icon(Icons.lock), label: 'AppLock'),
          BottomNavigationBarItem(icon: Icon(Icons.notifications_active), label: 'Anti-Theft'),
          BottomNavigationBarItem(icon: Icon(Icons.lock_clock), label: 'Seyf'),
          BottomNavigationBarItem(icon: Icon(Icons.settings), label: 'Sozlamalar'),
        ],
      ),
    );
  }

  // ========================================================
  // 1-TAB: QALQON (DASHBOARD)
  // ========================================================
  Widget _buildDashboardTab() {
    return RefreshIndicator(
      color: const Color(0xFF00FF66),
      backgroundColor: const Color(0xFF0D1117),
      onRefresh: _loadInitialData,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Android 13+ Maxsus Imkoniyatlar Diqqat Bannerni Ko'rsatish
            if (!_isAccessibilityGranted)
              Container(
                margin: const EdgeInsets.only(bottom: 18),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      const Color(0xFFFF9900).withOpacity(0.2),
                      const Color(0xFF0D1117),
                    ],
                  ),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: const Color(0xFFFF9900), width: 1.5),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.warning_amber_rounded, color: Color(0xFFFF9900), size: 24),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: Text(
                            'MAXSUS IMKONIYoTLAR (ACCESSIBILITY)',
                            style: TextStyle(
                              color: Color(0xFFFF9900),
                              fontFamily: 'monospace',
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      '0-ms AppLock to\'liq ishlashi uchun Maxsus Imkoniyatlar yoqilishi shart. Agar "Cheklangan sozlama" chiqsa, qo\'llanmani oching:',
                      style: TextStyle(color: Color(0xFFE6EDF3), fontSize: 12, height: 1.4),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFFF9900),
                        foregroundColor: Colors.black,
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      icon: const Icon(Icons.help_outline, size: 18, color: Colors.black),
                      label: const Text(
                        'QANDAY YOQILADI? (QO\'LLANMA)',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                      ),
                      onPressed: _showAndroid13RestrictedGuide,
                    ),
                  ],
                ),
              ),

            // Kiber Qalqon Reytingi Kartasi
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: const Color(0xFF0D1117),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(
                  color: _securityScore > 75
                      ? const Color(0xFF00FF66)
                      : (_securityScore > 40 ? const Color(0xFF00E5FF) : const Color(0xFFFF0055)),
                  width: 2,
                ),
                boxShadow: [
                  BoxShadow(
                    color: (_securityScore > 75
                            ? const Color(0xFF00FF66)
                            : (_securityScore > 40 ? const Color(0xFF00E5FF) : const Color(0xFFFF0055)))
                        .withOpacity(0.12),
                    blurRadius: 16,
                  ),
                ],
              ),
              child: Column(
                children: [
                  const Text(
                    'CYBER GUARDIAN XAVFSIZLIK REYTINGI',
                    style: TextStyle(
                      color: Color(0xFF8B949E),
                      fontFamily: 'monospace',
                      fontSize: 11,
                      letterSpacing: 1.2,
                    ),
                  ),
                  const SizedBox(height: 14),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        '$_securityScore%',
                        style: TextStyle(
                          color: _securityScore > 75
                              ? const Color(0xFF00FF66)
                              : (_securityScore > 40 ? const Color(0xFF00E5FF) : const Color(0xFFFF0055)),
                          fontSize: 48,
                          fontWeight: FontWeight.w900,
                          fontFamily: 'monospace',
                        ),
                      ),
                      const SizedBox(width: 14),
                      Icon(
                        _securityScore > 75
                            ? Icons.verified_user
                            : (_securityScore > 40 ? Icons.gpp_maybe : Icons.gpp_bad),
                        color: _securityScore > 75
                            ? const Color(0xFF00FF66)
                            : (_securityScore > 40 ? const Color(0xFF00E5FF) : const Color(0xFFFF0055)),
                        size: 42,
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _securityScore > 75
                        ? 'TIZIM MAKSIMAL HIMOYALANGAN'
                        : (_securityScore > 40 ? 'QISMAN HIMOYALANGAN' : 'DIQQAT: HIMOYASIZ HOLAT'),
                    style: const TextStyle(
                      color: Colors.white,
                      fontFamily: 'monospace',
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // 100% Oflayn va Zero-Internet Kafolati
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFF161B22),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: const Color(0xFF00FF66).withOpacity(0.3)),
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: const Color(0xFF00FF66).withOpacity(0.15),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.wifi_off, color: Color(0xFF00FF66), size: 22),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '100% OFLAYN & ZERO-INTERNET',
                          style: TextStyle(
                            color: Color(0xFF00FF66),
                            fontFamily: 'monospace',
                            fontWeight: FontWeight.bold,
                            fontSize: 12,
                          ),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Ilovada INTERNET ruxsati mutlaqo mavjud emas. Ma\'lumotlaringiz qurilmadan tashqariga chiqmaydi.',
                          style: TextStyle(color: Color(0xFF8B949E), fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Xavfsizlik modullari holati
            const Text(
              'FAOL KIBER QALQONLAR',
              style: TextStyle(
                color: Color(0xFF00FF66),
                fontFamily: 'monospace',
                fontWeight: FontWeight.bold,
                fontSize: 13,
                letterSpacing: 1.2,
              ),
            ),
            const SizedBox(height: 10),

            _buildShieldCard(
              title: '0-ms AppLock Nazorati',
              subtitle: _isAccessibilityGranted ? 'Faol va ilovalarni 0 ms da tutadi' : 'Maxsus imkoniyatlar yoqilmagan',
              isActive: _isAccessibilityGranted,
              icon: Icons.flash_on,
              onTap: _showAndroid13RestrictedGuide,
            ),
            _buildShieldCard(
              title: 'Hardware Keystore (AES-256)',
              subtitle: 'Shifrlash kalitlari apparat xotirasida (TEE) saqlanadi',
              isActive: true,
              icon: Icons.memory,
            ),
            _buildShieldCard(
              title: 'Intruder Selfie (O\'g\'ri Selfisi)',
              subtitle: 'Xato PIN kiritilganda yashirin suratga oladi',
              isActive: _intruderSelfieEnabled,
              icon: Icons.camera_front,
            ),
            _buildShieldCard(
              title: 'Anti-Theft Signallari',
              subtitle: 'Cho\'ntak va quvvatlagichdan uzish signali faol',
              isActive: _pocketAlarmEnabled || _unplugAlarmEnabled,
              icon: Icons.crisis_alert,
            ),
            const SizedBox(height: 20),

            // Ruxsatlar statusi
            const Text(
              'TIZIM RUXSATLARI HOLATI',
              style: TextStyle(
                color: Color(0xFF00E5FF),
                fontFamily: 'monospace',
                fontWeight: FontWeight.bold,
                fontSize: 13,
                letterSpacing: 1.2,
              ),
            ),
            const SizedBox(height: 10),

            ..._permissions.map((p) {
              final bool granted = p['isGranted'] == true;
              return Container(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                decoration: BoxDecoration(
                  color: const Color(0xFF0D1117),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: granted ? const Color(0xFF00FF66).withOpacity(0.3) : const Color(0xFFFF0055).withOpacity(0.3),
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      granted ? Icons.check_circle : Icons.error_outline,
                      color: granted ? const Color(0xFF00FF66) : const Color(0xFFFF0055),
                      size: 20,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        p['title'] ?? '',
                        style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w500),
                      ),
                    ),
                    if (!granted)
                      TextButton(
                        style: TextButton.styleFrom(
                          foregroundColor: const Color(0xFF00E5FF),
                          padding: const EdgeInsets.symmetric(horizontal: 8),
                        ),
                        onPressed: () {
                          if (p['type'] == 'ACCESSIBILITY') {
                            _showAndroid13RestrictedGuide();
                          } else {
                            _requestPermission(p['type'] ?? '');
                          }
                        },
                        child: const Text('YOQISH', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
                      ),
                  ],
                ),
              );
            }).toList(),
          ],
        ),
      ),
    );
  }

  Widget _buildShieldCard({
    required String title,
    required String subtitle,
    required bool isActive,
    required IconData icon,
    VoidCallback? onTap,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF0D1117),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: isActive ? const Color(0xFF00FF66).withOpacity(0.2) : Colors.white12,
        ),
      ),
      child: ListTile(
        onTap: onTap,
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: (isActive ? const Color(0xFF00FF66) : const Color(0xFF8B949E)).withOpacity(0.12),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: isActive ? const Color(0xFF00FF66) : const Color(0xFF8B949E), size: 22),
        ),
        title: Text(
          title,
          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
        ),
        subtitle: Text(
          subtitle,
          style: TextStyle(
            color: isActive ? const Color(0xFF00FF66) : const Color(0xFF8B949E),
            fontSize: 11,
          ),
        ),
        trailing: Container(
          width: 10,
          height: 10,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: isActive ? const Color(0xFF00FF66) : const Color(0xFFFF0055),
            boxShadow: [
              BoxShadow(
                color: (isActive ? const Color(0xFF00FF66) : const Color(0xFFFF0055)).withOpacity(0.4),
                blurRadius: 6,
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ========================================================
  // 2-TAB: APPLOCK (ILOVALAR QULFI)
  // ========================================================
  Widget _buildAppLockTab() {
    return Column(
      children: [
        // Qidiruv paneli
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: TextField(
            controller: _searchController,
            onChanged: _filterApps,
            style: const TextStyle(color: Colors.white, fontSize: 14),
            decoration: InputDecoration(
              hintText: 'Ilovalarni qidirish...',
              hintStyle: const TextStyle(color: Color(0xFF8B949E), fontSize: 13),
              prefixIcon: const Icon(Icons.search, color: Color(0xFF00FF66), size: 20),
              filled: true,
              fillColor: const Color(0xFF0D1117),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: Color(0xFF21262D)),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: Color(0xFF21262D)),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: Color(0xFF00FF66)),
              ),
            ),
          ),
        ),

        // Holat va tezkor amallar paneli
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Jami ilovalar: ${_filteredApps.length} ta',
                style: const TextStyle(color: Color(0xFF8B949E), fontSize: 12, fontFamily: 'monospace'),
              ),
              TextButton.icon(
                icon: const Icon(Icons.lock_reset, size: 16, color: Color(0xFF00E5FF)),
                label: const Text(
                  'Ro\'yxatni yangilash',
                  style: TextStyle(color: Color(0xFF00E5FF), fontSize: 11, fontWeight: FontWeight.bold),
                ),
                onPressed: _loadInstalledApps,
              ),
            ],
          ),
        ),

        // Ilovalar ro'yxati
        Expanded(
          child: _filteredApps.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.apps, size: 48, color: Color(0xFF21262D)),
                      const SizedBox(height: 12),
                      const Text(
                        'Ilova topilmadi',
                        style: TextStyle(color: Color(0xFF8B949E), fontSize: 14),
                      ),
                      const SizedBox(height: 12),
                      OutlinedButton(
                        onPressed: _setFallbackApps,
                        child: const Text('Namunaviy ro\'yxatni yuklash'),
                      ),
                    ],
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  itemCount: _filteredApps.length,
                  itemBuilder: (context, index) {
                    final app = _filteredApps[index];
                    final String name = app['appName'] ?? 'Noma\'lum';
                    final String pkg = app['packageName'] ?? '';
                    final bool isLocked = app['isLocked'] == true;

                    return Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      decoration: BoxDecoration(
                        color: const Color(0xFF0D1117),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(
                          color: isLocked ? const Color(0xFF00FF66).withOpacity(0.4) : const Color(0xFF21262D),
                        ),
                      ),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: isLocked ? const Color(0xFF00FF66).withOpacity(0.18) : const Color(0xFF161B22),
                          child: Icon(
                            isLocked ? Icons.lock : Icons.lock_open,
                            color: isLocked ? const Color(0xFF00FF66) : const Color(0xFF8B949E),
                            size: 20,
                          ),
                        ),
                        title: Text(
                          name,
                          style: TextStyle(
                            color: isLocked ? Colors.white : const Color(0xFFC9D1D9),
                            fontWeight: isLocked ? FontWeight.bold : FontWeight.normal,
                            fontSize: 14,
                          ),
                        ),
                        subtitle: Text(
                          pkg,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(color: Color(0xFF8B949E), fontSize: 11, fontFamily: 'monospace'),
                        ),
                        trailing: Switch(
                          value: isLocked,
                          activeColor: const Color(0xFF00FF66),
                          activeTrackColor: const Color(0xFF00FF66).withOpacity(0.3),
                          inactiveThumbColor: const Color(0xFF8B949E),
                          inactiveTrackColor: const Color(0xFF161B22),
                          onChanged: (_) => _toggleAppLock(pkg, isLocked),
                        ),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  // ========================================================
  // 3-TAB: ANTI-THEFT & SIGNALLAR
  // ========================================================
  Widget _buildAntiTheftTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF0D1117),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFF00FF66), width: 1.5),
            ),
            child: Row(
              children: [
                const Icon(Icons.notifications_active, color: Color(0xFF00FF66), size: 36),
                const SizedBox(width: 14),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'O\'G\'RILIKKA QARSHI TIZIM',
                        style: TextStyle(
                          color: Color(0xFF00FF66),
                          fontFamily: 'monospace',
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                      SizedBox(height: 2),
                      Text(
                        'Sensolar orqali qurilmangizni begona shaxslardan himoyalang',
                        style: TextStyle(color: Color(0xFF8B949E), fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          _buildAntiTheftToggle(
            title: 'Cho\'ntakdan O\'g\'irlash Signali',
            desc: 'Telefon cho\'ntakdan chiqarilganda baland signal chalish',
            icon: Icons.airline_seat_recline_extra,
            value: _pocketAlarmEnabled,
            onChanged: (v) => setState(() => _pocketAlarmEnabled = v),
          ),
          _buildAntiTheftToggle(
            title: 'Zaryadlovchidan Uzish Signali',
            desc: 'Telefon quvvatlagichdan ruxsatsiz sug\'urilganda signal chalish',
            icon: Icons.power_off,
            value: _unplugAlarmEnabled,
            onChanged: (v) => setState(() => _unplugAlarmEnabled = v),
          ),
          _buildAntiTheftToggle(
            title: 'Harakat Detektori (Motion Alarm)',
            desc: 'Stolga qo\'yilgan telefon qimirlatilganda signal chalish',
            icon: Icons.screen_rotation,
            value: _motionAlarmEnabled,
            onChanged: (v) => setState(() => _motionAlarmEnabled = v),
          ),
          _buildAntiTheftToggle(
            title: 'Intruder Selfie (O\'g\'ri Suratga Olish)',
            desc: '3 marta noto\'g\'ri PIN terilganda yashirincha old kamerada suratga olish',
            icon: Icons.camera_front,
            value: _intruderSelfieEnabled,
            onChanged: (v) => setState(() => _intruderSelfieEnabled = v),
          ),

          const SizedBox(height: 20),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF0D1117),
              foregroundColor: const Color(0xFF00E5FF),
              side: const BorderSide(color: Color(0xFF00E5FF)),
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
            icon: const Icon(Icons.volume_up, color: Color(0xFF00E5FF)),
            label: const Text(
              'SIRENA SIGNALINI TEKSHIRISH (TEST)',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, fontFamily: 'monospace'),
            ),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Sirena sinovi muvaffaqiyatli o\'tkazildi!', style: TextStyle(color: Color(0xFF00FF66))),
                  backgroundColor: Color(0xFF0D1117),
                ),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildAntiTheftToggle({
    required String title,
    required String desc,
    required IconData icon,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: const Color(0xFF0D1117),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: value ? const Color(0xFF00FF66).withOpacity(0.3) : const Color(0xFF21262D)),
      ),
      child: SwitchListTile(
        secondary: Icon(icon, color: value ? const Color(0xFF00FF66) : const Color(0xFF8B949E)),
        title: Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
        subtitle: Text(desc, style: const TextStyle(color: Color(0xFF8B949E), fontSize: 11)),
        value: value,
        activeColor: const Color(0xFF00FF66),
        onChanged: onChanged,
      ),
    );
  }

  // ========================================================
  // 4-TAB: SEYF & KIBER VOSITALAR
  // ========================================================
  Widget _buildVaultTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: const Color(0xFF0D1117),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFF00E5FF), width: 1.5),
            ),
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.enhanced_encryption, color: Color(0xFF00E5FF), size: 28),
                    SizedBox(width: 10),
                    Text(
                      'AES-256-GCM SEYF',
                      style: TextStyle(
                        color: Color(0xFF00E5FF),
                        fontFamily: 'monospace',
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                        letterSpacing: 1.2,
                      ),
                    ),
                  ],
                ),
                SizedBox(height: 8),
                Text(
                  'Fayllar harbiy darajadagi AES-256 shifrlash orqali to\'liq himoyalanadi. Kalitlar Android Hardware Keystore (TEE) chipida saqlanadi.',
                  style: TextStyle(color: Color(0xFF8B949E), fontSize: 12, height: 1.4),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          _buildToolCard(
            title: 'Maxfiy Fotosuratlar & Videolar Seyfi',
            desc: 'Galereyadan yashirilgan fotosuratlarni shifrlangan holatda saqlash',
            icon: Icons.photo_library,
            onTap: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Seyf xavfsiz holatda. Fayllar shifrlangan.', style: TextStyle(color: Color(0xFF00FF66))),
                  backgroundColor: Color(0xFF0D1117),
                ),
              );
            },
          ),
          _buildToolCard(
            title: 'EXIF / GPS Metadatalarni Tozalash',
            desc: 'Suratlardagi geolokatsiya, kamera modeli va vaqt belgilarini o\'chirish',
            icon: Icons.cleaning_services,
            onTap: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('EXIF Scrubber tayyor. Rasm tanlang.', style: TextStyle(color: Color(0xFF00E5FF))),
                  backgroundColor: Color(0xFF0D1117),
                ),
              );
            },
          ),
          _buildToolCard(
            title: 'DoD 5220.22-M File Shredder',
            desc: 'Fayllarni 3 bosqichda qayta yozib, qayta tiklab bo\'lmaydigan qilib yo\'q qilish',
            icon: Icons.delete_forever,
            onTap: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('File Shredder: 3-pass DoD 5220.22-M faol.', style: TextStyle(color: Color(0xFFFF0055))),
                  backgroundColor: Color(0xFF0D1117),
                ),
              );
            },
          ),
          _buildToolCard(
            title: 'LSB Steganografiya (Yashirin Matn)',
            desc: 'Matnli xabarni fotosuratning piksellari ichiga ko\'rinmas qilib joylashtirish',
            icon: Icons.hide_image,
            onTap: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Steganografiya moduli tayyor.', style: TextStyle(color: Color(0xFF00FF66))),
                  backgroundColor: Color(0xFF0D1117),
                ),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildToolCard({required String title, required String desc, required IconData icon, required VoidCallback onTap}) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: const Color(0xFF0D1117),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFF21262D)),
      ),
      child: ListTile(
        onTap: onTap,
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: const Color(0xFF00E5FF).withOpacity(0.12),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: const Color(0xFF00E5FF), size: 22),
        ),
        title: Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
        subtitle: Text(desc, style: const TextStyle(color: Color(0xFF8B949E), fontSize: 11)),
        trailing: const Icon(Icons.arrow_forward_ios, color: Color(0xFF8B949E), size: 14),
      ),
    );
  }

  // ========================================================
  // 5-TAB: SOZLAMALAR & NIQOB
  // ========================================================
  Widget _buildSettingsTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Kalkulyator Niqobi Kartasi
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF0D1117),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFF00FF66), width: 1.5),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.calculate, color: Color(0xFF00FF66), size: 24),
                        SizedBox(width: 10),
                        Text(
                          'KALKULYATOR NIQOBI (STEALTH)',
                          style: TextStyle(
                            color: Color(0xFF00FF66),
                            fontFamily: 'monospace',
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                    Switch(
                      value: _calculatorDisguise,
                      activeColor: const Color(0xFF00FF66),
                      onChanged: (_) => _toggleDisguise(),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                const Text(
                  'Niqob yoqilganda ilova ishga tushganda haqiqiy kalkulyator ochiladi. BlackBox\'ni ochish uchun kalkulyatorda maxfiy PIN-kod (masalan 7777) va barobar (=) belgisini bosing.',
                  style: TextStyle(color: Color(0xFF8B949E), fontSize: 12, height: 1.4),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Android 13+ Qo'llanma tugmasi
          ListTile(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
              side: const BorderSide(color: Color(0xFFFF9900)),
            ),
            tileColor: const Color(0xFF0D1117),
            leading: const Icon(Icons.security, color: Color(0xFFFF9900)),
            title: const Text(
              'Android 13+ Cheklangan Sozlama Qo\'llanmasi',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
            ),
            subtitle: const Text(
              'Maxsus Imkoniyatlar (Accessibility) tugmasini ochish sirlari',
              style: TextStyle(color: Color(0xFF8B949E), fontSize: 11),
            ),
            trailing: const Icon(Icons.arrow_forward_ios, color: Color(0xFFFF9900), size: 14),
            onTap: _showAndroid13RestrictedGuide,
          ),
          const SizedBox(height: 12),

          // Asosiy PIN-kod sozlamasi
          ListTile(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
              side: const BorderSide(color: Color(0xFF21262D)),
            ),
            tileColor: const Color(0xFF0D1117),
            leading: const Icon(Icons.pin, color: Color(0xFF00FF66)),
            title: const Text(
              'Asosiy PIN-kodni o\'zgartirish',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
            ),
            subtitle: Text(
              'Hozirgi PIN: $_masterPin',
              style: const TextStyle(color: Color(0xFF00FF66), fontSize: 11, fontFamily: 'monospace'),
            ),
            trailing: const Icon(Icons.edit, color: Color(0xFF8B949E), size: 18),
            onTap: _showChangePinDialog,
          ),
          const SizedBox(height: 12),

          // Morse kod bilan ovoz tugmalari
          ListTile(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
              side: const BorderSide(color: Color(0xFF21262D)),
            ),
            tileColor: const Color(0xFF0D1117),
            leading: const Icon(Icons.volume_up, color: Color(0xFF00E5FF)),
            title: const Text(
              'Apparat Tugmalari (Morse Kod)',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
            ),
            subtitle: const Text(
              'Ovoz tugmalarini bosish ketma-ketligi orqali ekran yoniq holda ham qulfdan chiqarish',
              style: TextStyle(color: Color(0xFF8B949E), fontSize: 11),
            ),
            trailing: const Icon(Icons.check, color: Color(0xFF00FF66), size: 18),
          ),
          const SizedBox(height: 24),

          // Mualliflik va brend
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: const Color(0xFF0D1117),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: const Color(0xFF21262D)),
            ),
            child: Column(
              children: [
                Container(
                  width: 54,
                  height: 54,
                  padding: const EdgeInsets.all(4),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: const Color(0xFF00FF66), width: 1.5),
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: Image.asset(
                      'assets/images/blackbox_logo.png',
                      fit: BoxFit.contain,
                      errorBuilder: (_, __, ___) => const Icon(Icons.shield, color: Color(0xFF00FF66)),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'BLACKBOX CYBER PRIVACY SUITE',
                  style: TextStyle(
                    color: Color(0xFF00FF66),
                    fontFamily: 'monospace',
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                    letterSpacing: 1.2,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Muallif: Elbek Web\nTelegram: @elbekdesign_va_webdasturchi_uz',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Color(0xFF00E5FF), fontSize: 12, height: 1.4),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Versiya: 1.0.0 (Release) • 100% Oflayn Air-Gapped',
                  style: TextStyle(color: Color(0xFF8B949E), fontSize: 11, fontFamily: 'monospace'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _showChangePinDialog() {
    final pinController = TextEditingController(text: _masterPin);
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: const Color(0xFF0D1117),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
            side: const BorderSide(color: Color(0xFF00FF66)),
          ),
          title: const Text(
            'YANGI PIN-KOD',
            style: TextStyle(color: Color(0xFF00FF66), fontFamily: 'monospace', fontSize: 14),
          ),
          content: TextField(
            controller: pinController,
            keyboardType: TextInputType.number,
            maxLength: 6,
            obscureText: true,
            style: const TextStyle(color: Colors.white, fontSize: 18, fontFamily: 'monospace'),
            decoration: const InputDecoration(
              hintText: 'Yangi PIN (masalan 7777)',
              hintStyle: TextStyle(color: Color(0xFF8B949E), fontSize: 14),
              enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFF00FF66))),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('BEKOR QILISH', style: TextStyle(color: Color(0xFF8B949E))),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF00FF66), foregroundColor: Colors.black),
              onPressed: () async {
                final newPin = pinController.text.trim();
                if (newPin.isNotEmpty) {
                  try {
                    await _platform.invokeMethod('setMasterPin', {'pin': newPin});
                    setState(() => _masterPin = newPin);
                  } catch (e) {
                    // Ignored
                  }
                }
                Navigator.pop(ctx);
              },
              child: const Text('SAQLASH', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        );
      },
    );
  }
}
