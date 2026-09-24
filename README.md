# android_admin_common

小学生向け学習アプリで共通利用する Android Compose ライブラリです。

学習アプリ本体とは切り離し、保護者/管理者向けのログイン、PIN 入力、
WiFi 設定、YouTube 再生画面など、複数アプリで同じ品質にそろえたい
コンポーネントをまとめています。

## 目的

- 小学生が使う学習アプリで、管理者向け機能の UI と挙動を統一する。
- アプリごとの重複実装を減らし、保護者設定や端末設定まわりの保守をしやすくする。
- 学習内容や報酬ルールなど、アプリ固有のロジックとは分離して再利用できる形にする。

## 想定している利用場面

- 保護者や先生が管理画面に入るためのログイン画面。
- 子どもが誤って管理機能を開かないための PIN 入力 UI。
- 学習用端末で使う WiFi の選択、保存、接続確認。
- 学習報酬や教材導線として使う YouTube WebView 画面。

## 使い方

利用するアプリの `settings.gradle.kts` に included build を追加します。

```kotlin
includeBuild("../android_admin_common")
```

次に、アプリモジュールからこのライブラリへ依存関係を追加します。

```kotlin
implementation("com.yuji:android-admin-common:0.1.0")
```

このライブラリは Android Library として提供され、Jetpack Compose / Material 3 を前提にしています。

## 現在共有している API

### 管理者 UI

- `AdminLoginScreen`: フルスクリーンの管理者ログイン UI。
- `AdminUnlockPanel`: 画面内に埋め込む保護者/管理者ロック解除パネル。
- `AdminPinDisplay`, `AdminNumericKeypad`: 管理者入力画面で使う、画面上に描画される
  PIN 入力コントロール。Android のソフトウェアキーボードを使わずに入力できます。

### WiFi 設定

- `AdminWifiSettingsSection`: WiFi SSID の選択、パスワード入力、接続テスト、
  保存/更新、保存済みネットワーク一覧をまとめた共有 UI。
- `AdminWifiNetwork`: アプリ境界でマッピングするための共有 WiFi 認証情報モデル。
- `hasFineLocationPermission()`, `currentWifiSsid()`, `availableWifiSsids()`,
  `requestAdminWifiConnection()`: 共有 UI で使う Android WiFi ヘルパー。

### YouTube 再生

- `YoutubePlayerScreen`: `school_kanji_game` の挙動に合わせた、アプリ内共有
  YouTube WebView 画面。戻るボタン、残り時間ヘッダー、WiFi 接続を前提にした読み込み、
  再生進捗ブリッジ、ライフサイクル停止時のクローズ処理を備えています。
- `rememberYoutubeWifiStatus()`: 共有 YouTube WiFi 候補の接続状態。
- `toYoutubeResumeUrl()`, `normalizedYoutubeUrl()`, `isYoutubeVideoUrl()`:
  共有 YouTube URL ヘルパー。

### セキュリティ/入力補助

- `adminSha256()`: 既存実装との互換性を保つ SHA-256 ヘルパー。
- `digitsOnly()`: 数字パスワード入力の正規化。
- `AdminPasswordHasher`: 新しい PIN 向けのソルト付きハッシュ作成と検証。

## アプリ側に残すもの

このライブラリは、共通化しやすい管理者向け UI と補助処理だけを担当します。
次のようなアプリ固有の仕様は、各学習アプリ側で管理してください。

- 学年、教科、単元などの学習コンテンツ設定。
- 漢字の学年フィルターや出題ルール。
- YouTube 視聴を報酬として扱う条件や時間制限。
- WiFi 接続後にどの画面へ遷移するかなど、アプリごとの画面遷移。
- 利用者データ、学習履歴、課金、分析などのアプリ固有データ。

## 開発方針

- 子どもが触る学習画面ではなく、保護者/管理者が扱う共通機能を中心に置く。
- 画面部品は Compose で実装し、各アプリから組み込みやすい API にする。
- アプリ固有のルールをこのライブラリへ持ち込まず、必要な値や処理は呼び出し側から渡す。
- 既存アプリとの互換性が必要な処理は、用途を明確にしたうえで共有 API として残す。

## 動作環境

- Android minSdk 23
- Android compileSdk 35
- Kotlin Compose Compiler Plugin 2.4.0
- Jetpack Compose BOM 2024.12.01
