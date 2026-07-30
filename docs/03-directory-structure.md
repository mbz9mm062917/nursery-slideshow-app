# ディレクトリ構成

## 1. フロントエンド（`frontend/`）

```
frontend/
├── public/
├── src/
│   ├── assets/                 # 画像・アイコン・グローバルCSS
│   ├── components/
│   │   ├── common/             # Button / Icon / ProgressBar など画面共通の汎用UIパーツ
│   │   └── wizard/             # 各工程専用の複合コンポーネント（PhotoUploader, PhotoSorter, ThemePicker 等）
│   ├── composables/            # useFileValidation, useJobPolling などロジックの再利用単位
│   ├── views/                  # ルートと1対1対応する画面コンポーネント
│   │   ├── HomeView.vue
│   │   ├── PhotoUploadView.vue
│   │   ├── PhotoReorderView.vue
│   │   ├── TitleInputView.vue
│   │   ├── ThemeSelectView.vue
│   │   ├── BgmSelectView.vue
│   │   ├── DurationSelectView.vue
│   │   ├── PreviewView.vue
│   │   ├── VideoGeneratingView.vue
│   │   └── DownloadView.vue
│   ├── router/
│   │   └── index.ts            # ウィザードの遷移順をルート定義として表現
│   ├── stores/
│   │   ├── projectStore.ts     # 編集中スライドショーの状態（写真・タイトル・テーマ・BGM・時間）
│   │   └── videoJobStore.ts    # 動画生成ジョブの進捗状態
│   ├── api/
│   │   ├── httpClient.ts       # fetch/axiosの共通設定（baseURL, エラーハンドリング）
│   │   ├── projectApi.ts
│   │   ├── photoApi.ts
│   │   ├── videoJobApi.ts
│   │   ├── themeApi.ts
│   │   └── bgmApi.ts
│   ├── types/                  # APIレスポンス・ドメインモデルの型定義
│   ├── constants/              # スライド時間の選択肢など画面共通の定数
│   ├── utils/                  # 汎用関数（ファイルサイズ表示、バリデーション等）
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

**設計理由**

- `views/` を工程数と1対1にすることで、「今どの画面にいるか＝何をすべきか」がURLレベルで明確になる（保育士の迷いにくさに直結）。
- 画面固有の複雑なUI（ドラッグ並び替え等）は `components/wizard/` に切り出し、`views/` は「配置とstoreの受け渡し」に専念させ、テストしやすくする。
- `api/` を機能単位でファイル分割することで、バックエンドのAPI変更の影響範囲を1ファイルに閉じる。
- `stores/` を2つに分けたのは責務が異なるため：`projectStore` は「編集内容」、`videoJobStore` は「非同期処理の進捗」。混在させると、生成中に編集操作をブロックする等の制御が複雑化する。

## 2. バックエンド（`backend/`）

Java/Spring Bootでは、**レイヤー単位ではなく機能（feature）単位** でパッケージを分割する。
機能が増えるほど「全Controllerが1パッケージに集まる」形式は見通しが悪くなるため、実務では機能単位分割が主流。

```
backend/
├── src/main/java/com/nursery/slideshow/
│   ├── SlideshowApplication.java
│   ├── common/
│   │   ├── exception/           # GlobalExceptionHandler, 業務例外クラス
│   │   ├── config/              # AsyncConfig, CorsConfig, WebMvcConfig
│   │   └── storage/             # StorageService(interface) / LocalStorageService(実装)
│   ├── project/
│   │   ├── ProjectController.java
│   │   ├── ProjectService.java
│   │   ├── ProjectRepository.java
│   │   ├── Project.java
│   │   └── dto/
│   ├── photo/
│   │   ├── PhotoController.java
│   │   ├── PhotoService.java
│   │   ├── PhotoRepository.java
│   │   ├── Photo.java
│   │   └── dto/
│   ├── videojob/
│   │   ├── VideoJobController.java
│   │   ├── VideoJobService.java        # ジョブの作成・状態管理
│   │   ├── VideoGenerationService.java # FFmpeg実行ロジック（@Async）
│   │   ├── VideoJobRepository.java
│   │   ├── VideoJob.java
│   │   ├── VideoJobStatus.java         # Enum
│   │   ├── theme/                      # テーマごとのFFmpeg描画ロジック（Strategyパターン）
│   │   │   ├── ThemeRenderer.java      # interface
│   │   │   ├── SimpleThemeRenderer.java
│   │   │   ├── CuteThemeRenderer.java
│   │   │   ├── GraduationThemeRenderer.java
│   │   │   └── SportsThemeRenderer.java
│   │   └── dto/
│   ├── theme/
│   │   ├── ThemeController.java
│   │   ├── ThemeService.java
│   │   ├── ThemeRepository.java
│   │   └── Theme.java
│   └── bgm/
│       ├── BgmController.java
│       ├── BgmService.java
│       ├── BgmRepository.java
│       └── Bgm.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/            # Flyway: V1__init_schema.sql, V2__seed_themes.sql ...
├── src/test/java/com/nursery/slideshow/...   # featureごとにテストを対応配置
└── build.gradle.kts
```

**設計理由**

- 機能単位パッケージにより、「BGM機能を修正したい」ときに触るファイルが `bgm/` 配下に閉じる。将来AI編集等を追加する際も新パッケージを追加するだけで済み、既存コードへの影響を最小化できる。
- `common/storage/` に `StorageService` インターフェースを置くことで、`photo` や `videojob` は「保存先がローカルかS3か」を意識せず利用できる（システム設計Step2の方針を実装レベルに落とし込み）。
- `videojob` パッケージ内で「ジョブの状態管理（`VideoJobService`）」と「実際のFFmpeg実行（`VideoGenerationService`）」を分けているのは、将来ジョブキューの実装を差し替える際に実行ロジック本体（FFmpegコマンド組み立て）を変更せずに済むようにするため。
- テーマごとの見た目・演出差分は、DB設計をシンプルに保つためJSON設定ではなく `videojob/theme/` 配下の `ThemeRenderer` 実装（Strategyパターン）としてコードで表現する（テーマ数が少ない現段階ではDBのJSON管理よりも型安全でシンプル）。
- Flywayを導入し `db/migration/` でスキーマ変更を履歴管理する。手動DDL運用は事故の元になりやすく、実務標準として採用する。
