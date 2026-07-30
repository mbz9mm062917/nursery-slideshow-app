# API設計

## 1. 設計方針

- ベースパス: `/api`
- 認証なし（MVP方針どおり）。プロジェクトIDにUUIDを用いることでアクセスの推測しにくさを担保する。
- リクエスト/レスポンスは基本JSON（写真アップロードのみ`multipart/form-data`）。
- 命名は「コレクション（複数形）」＋「親子関係はネスト」の標準的なREST規約に従う。ただし`photoId`・`jobId`のようにグローバルに一意なIDを持つ子リソースへの単体操作（削除・ファイル取得など）は、親をネストせずフラットなパスにする（例: `DELETE /api/photos/{photoId}`）。これによりパスが不必要に長くならず、フロントの実装もシンプルになる。
- エラーレスポンスは共通フォーマットに統一する。

  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "タイトルを入力してください"
  }
  ```

  `message`は`GlobalExceptionHandler`（`common/exception`）が保育士向けにそのまま画面表示できる日本語文言へ変換したものを返す。内部的な例外詳細はログにのみ出力し、レスポンスには含めない。

## 2. Project API

| Method | Path | 概要 |
|---|---|---|
| POST | `/api/projects` | 新規プロジェクト作成(ウィザード開始時に呼び出す) |
| GET | `/api/projects` | プロジェクト一覧取得(※本番ウィザードUIでは未使用。フロントエンド実装時の疎通確認・開発用) |
| GET | `/api/projects/{projectId}` | プロジェクトの現在の状態を取得(画面リロード時の復元に使用) |
| PATCH | `/api/projects/{projectId}` | タイトル/テーマ/BGM/スライド時間を部分更新 |
| DELETE | `/api/projects/{projectId}` | プロジェクト削除(※本番ウィザードUIでは未使用。開発用・データ整理用) |

**設計理由**: タイトル・テーマ・BGM・時間の4項目それぞれに専用エンドポイントを作ると画面数分エンドポイントが増えてしまう。ウィザードの各画面は「1項目を更新して次へ進む」という同じパターンのため、`PATCH`1本に集約しリクエストボディの差分だけで表現する（過度なエンドポイント分割を避ける）。

`PATCH`リクエスト例（テーマ選択画面から）:
```json
{ "themeCode": "cute" }
```

`GET /api/projects/{projectId}` レスポンス例:
```json
{
  "id": "b3f1...",
  "title": "さくら組 運動会",
  "themeCode": "cute",
  "bgmCode": "energetic",
  "slideDurationSec": 5,
  "photoCount": 42,
  "createdAt": "2026-07-20T10:00:00"
}
```

## 3. Photo API

| Method | Path | 概要 |
|---|---|---|
| POST | `/api/projects/{projectId}/photos` | 写真の複数アップロード（multipart） |
| GET | `/api/projects/{projectId}/photos` | 写真一覧取得（`display_order`順） |
| PUT | `/api/projects/{projectId}/photos/order` | 並び替え結果を一括反映 |
| DELETE | `/api/photos/{photoId}` | 写真を1枚削除（アップロードミスの取り消し用） |
| GET | `/api/photos/{photoId}/file` | 画像ファイル本体を返す（プレビュー表示用） |

**設計理由**

- アップロードは複数ファイルを一度に受け取り、サーバー側でjpg/jpeg/png形式チェック・件数上限（300枚）チェックを行い、違反があれば400と分かりやすいメッセージを返す。
- 並び替えは差分PATCHではなく`PUT .../order`でプロジェクト内の全並び順を一括置き換えする。理由はデータベース設計（Step4）で決めた「並び替えはトランザクション内で一括更新し、DBのUNIQUE制約に頼らない」という方針をAPIレベルでもそのまま表現するため。

  リクエスト例:
  ```json
  { "photoIds": [103, 101, 102, 104] }
  ```
  サーバーは、渡された配列が「そのプロジェクトの全写真IDの過不足ない並び替え」であることを検証してから`display_order`を0始まりで振り直す。検証に失敗した場合は400を返す。

- `GET /api/photos/{photoId}/file` を用意した理由は、`storage_key`（DB内部の論理パス）をフロントに直接渡さず、必ずバックエンド経由でStorageServiceが解決した実体（ローカルファイル or 将来のS3オブジェクト）をストリーミングするため。これによりフロントは常に同じURL形式で画像を参照でき、将来ストレージをS3に切り替えてもフロントのコード変更が不要になる。

## 4. Theme API / BGM API（参照専用）

| Method | Path | 概要 |
|---|---|---|
| GET | `/api/themes` | 有効なテーマ一覧（`sort_order`順） |
| GET | `/api/themes/{themeId}/thumbnail` | テーマのサムネイル画像 |
| GET | `/api/bgms` | 有効なBGM一覧（`sort_order`順） |
| GET | `/api/bgms/{bgmId}/file` | BGM音源ファイル（選択画面でのプレビュー再生にも流用） |

**設計理由**: テーマ・BGMはFlywayのseedデータで管理するマスタ情報であり、MVPでは作成・更新用のAPI（管理画面等）は不要と判断し、参照系のみ用意する。BGM選択画面での試聴も、専用のプレビューAPIを新設せず、この音源ファイルURLをそのまま`<audio>`タグに渡すことで実現でき、エンドポイントを増やさずに済む。

## 5. Video Job API

| Method | Path | 概要 |
|---|---|---|
| POST | `/api/projects/{projectId}/video-jobs` | 動画生成を開始する |
| GET | `/api/video-jobs/{jobId}` | ジョブの状態・進捗を取得（ポーリング用） |
| GET | `/api/projects/{projectId}/video-jobs/latest` | 直近の完成動画のジョブ情報を取得（ダウンロード画面用） |
| GET | `/api/video-jobs/{jobId}/download` | 生成された動画ファイルをダウンロード |

**設計理由**

- `POST .../video-jobs`は、プロジェクトが生成可能な状態か（タイトル・テーマ・BGM・スライド時間が入力済み、写真が1枚以上ある）をサーバー側で検証する。不備があれば400、既に`PENDING`または`PROCESSING`のジョブが存在する場合は二重生成防止のため409を返す。検証を通過したら`video_jobs`に`PENDING`のレコードを作成し、`@Async`で処理を開始した上で即座に`jobId`を返す（Step2で決めた非同期方針そのまま）。

  レスポンス例（202 Accepted）:
  ```json
  { "jobId": 501, "status": "PENDING" }
  ```

- `GET /api/video-jobs/{jobId}` はフロントが一定間隔（例: 2秒）でポーリングする想定。

  レスポンス例:
  ```json
  { "jobId": 501, "status": "PROCESSING", "progress": 63 }
  ```
  失敗時:
  ```json
  { "jobId": 501, "status": "FAILED", "progress": 0, "errorMessage": "動画の生成に失敗しました。もう一度お試しください。" }
  ```

- `GET .../video-jobs/latest` は、Step4で決定した「`latest_video_job_id`列を持たず`status=COMPLETED`を`completed_at`降順で検索する」方針をそのままAPI化したもの。ダウンロード画面はプロジェクトIDだけを知っていればよく、ジョブIDを別途管理する必要がない。
- ダウンロードは`jobId`単位のエンドポイントにすることで、「直近の動画をダウンロード」も「（将来の再生成後に）過去のジョブを個別にダウンロード」も同じ形で扱える。

## 6. 主なステータスコード

| コード | 用途 |
|---|---|
| 200 | 取得・更新成功 |
| 201 | 作成成功（プロジェクト作成、写真アップロード） |
| 202 | 動画生成の受付成功（非同期処理を開始した） |
| 400 | 入力不備（未対応のファイル形式、必須項目未入力、並び替えデータ不正など） |
| 404 | 指定したプロジェクト／写真／ジョブが存在しない |
| 409 | 既に動画生成中のプロジェクトに対して再度生成をリクエストした場合 |
| 500 | FFmpeg実行エラー等、サーバー内部エラー |
