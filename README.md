# CamGallery Hook

> **Note:** このモジュールとドキュメントは、AIアシスタント「Muse」によって作成されました。

Pixel（Google）カメラで撮影したあと、左下のサムネイルをタップすると通常はGoogleフォトが開きますが、それを任意のギャラリーアプリで開けるようにする LSPosed モジュールです。

## 仕組み

Googleカメラはレビュー画面を開くとき、宛先をGoogleフォト（`com.google.android.apps.photos/.pager.HostPhotoPagerActivity`）に固定したIntentを使います。本モジュールはカメラアプリのプロセスで `Instrumentation.execStartActivity` をフックし、

- 宛先がGoogleフォトのIntentを検出したら
- 明示コンポーネントとパッケージ指定を取り除き
- アクションを `android.provider.action.REVIEW` から `ACTION_VIEW` に書き換えます

これにより通常のIntent解決が働き、既定のギャラリーアプリ（または選択ダイアログ）で写真が開くようになります。Googleフォトやカメラ本体には一切変更を加えません。

## 要件

- libxposed API（API 101以降）対応の LSPosed
- Android 12 以上
- 検証環境: Pixel 8a / Android 16 / Googleカメラ 10.3.183 / SukiSU Ultra + Zygisk LSPosed v2.2.1

## 使い方

1. Releases から APK をダウンロードしてインストール
2. LSPosed マネージャーで「CamGallery Hook」を有効化
3. スコープで Googleカメラ（`com.google.android.GoogleCamera`）を選択（「おすすめ」に表示されます）
4. 端末を再起動（モジュールの読み込みに必要です）
5. カメラで撮影してサムネイルをタップ → 初回は開くアプリを選択し、「常時」を選ぶと固定されます

## 注意事項

- Googleカメラのアップデートで内部実装が変わると、動作しなくなる可能性があります
- 元に戻すには LSPosed でモジュールを無効化してください
- 本モジュールは現状のまま提供されます。利用は自己責任でお願いします

## ビルド

Gradle を使わず、JDK 17 と Android SDK（platform android-35 / build-tools）のみでビルドします。libxposed API 102 の `classes.jar` が別途必要です。

```sh
./build.sh
```

ツールチェーンの場所は `build.sh` 冒頭の `TOOLS` 変数で変更できます。
