# aws-emulator-poc

[AWS Emulator](https://github.com/aws/aws-emulator) を Docker で動かし、Spring Boot (Spring MVC) のアプリから S3 へ PUT / GET する PoC です。
リージョンは `ap-northeast-1` を使います。

「AWS アカウントなしで、S3 を使うアプリをローカルで動かしたい」「AWS Emulator を Docker Compose で試したい」といった用途を想定しています。

> **ご注意**
> - 非公式のサンプルであり、Amazon Web Services 社とは関係ありません。AWS、Amazon S3 は Amazon.com, Inc. またはその関連会社の商標です。
> - 検証用の PoC です。本番環境での利用は想定していません。AWS Emulator 自体も、本番ワークロードやセキュリティのテストには使わないよう案内されています([Security in AWS Emulator](https://docs.aws.amazon.com/emulator/latest/userguide/security.html))。
> - このリポジトリは AWS Emulator のバイナリを含みません。Docker イメージのビルド時に、AWS の配布サイトから RPM をダウンロードします。
> - AWS Emulator はテレメトリが既定で有効ですが、この PoC の `compose.yaml` では無効にしています(「テレメトリ」を参照)。

- Java 25 (LTS) / Spring Boot 4.2.0 / AWS SDK for Java v2 2.59.7 / Gradle 9.9.2
- AWS Emulator: AWS 公式の手順([Getting started with Docker](https://docs.aws.amazon.com/emulator/latest/userguide/getting-started-docker.html))で、Dockerfile から自分でイメージをビルドします(公式の事前ビルド済みイメージはありません)。

## 構成

```
compose.yaml                 AWS Emulator とアプリ
Dockerfile                   アプリのイメージ (Gradle でビルドし、Java 25 の JRE で実行)
docker/Dockerfile.emulator   AWS Emulator のイメージ (Amazon Linux 2023 + 公式 RPM)
docker/entrypoint.sh         エミュレータの CA 証明書を信頼ストアに取り込んで、アプリを起動
src/                         Spring Boot アプリ (Spring MVC + Thymeleaf)
build.gradle.kts             Gradle のビルド定義 (gradlew / gradle/ はそのラッパー)
.env.example                 compose が使う環境変数の一覧
```

### ネットワークの構成

AWS Emulator の証明書は `localhost` / `127.0.0.1` 向けです。そのため `app` は `network_mode: "service:emulator"` で
エミュレータとネットワークを共有し、`https://127.0.0.1:9297` に、証明書の検証を有効にしたまま接続します。
アプリのポート(8080)は、ネットワークを共有しているエミュレータ側で公開しています。ポートは両方とも `127.0.0.1` のみに公開します。

エミュレータが生成する認証情報と CA 証明書は、`.emulator/` に保存されます(`.gitignore` 済み)。アプリはそこを読み取り専用で参照します。

## 前提条件

- Docker (Docker Compose v2 が使えること)
- ポート 9297 (エミュレータ) と 8080 (アプリ) が空いていること(8080 は [`.env.example`](.env.example) の `APP_PORT` で変更できます)
- ローカルの Java は不要です(ビルドと実行は Docker 内で行います)。IDE で開発する場合のみ Java 25 が必要です。

## 起動

```sh
git clone <このリポジトリの URL>
cd <クローンしたディレクトリ>
mkdir -p .emulator
docker compose up -d --build
```

- **Intel / AMD のマシン**では、RPM のアーキテクチャを指定します: `RPM_ARCH=x86_64 docker compose up -d --build`
  (`.env.example` を `.env` にコピーして、`RPM_ARCH=x86_64` と書いてもかまいません)
- **Linux** では、`.env` に `LOCAL_UID` / `LOCAL_GID` を自分の値(`id -u` / `id -g`)で書いてください。`.emulator/` に生成されるファイルの所有者になります。

アプリは、起動時にバケット `poc-bucket` を `ap-northeast-1` に作成します。

起動後の確認例:

```sh
curl -X PUT -H 'Content-Type: text/plain' --data 'hello' localhost:8080/api/objects/hello.txt   # 201
curl localhost:8080/api/objects/hello.txt                                                         # hello
```

停止:

```sh
docker compose down   # エミュレートされたリソースはメモリ上にあるため、消えます
```

## 使い方

| 用途 | URL |
|---|---|
| 画面(アップロードと一覧、ダウンロード) | http://localhost:8080 |
| AWS Emulator のエンドポイント | https://127.0.0.1:9297 (自己署名証明書) |
| ヘルスチェック | https://127.0.0.1:9297/__admin/health |

### API

| メソッド | パス | 内容 |
|---|---|---|
| GET | `/` | 画面(バケット名とオブジェクトの一覧、アップロードフォーム) |
| POST | `/objects` | 画面から、ファイルを S3 に PUT する(ファイル名がキーになります) |
| GET | `/objects/{key}` | S3 から GET して、ダウンロードする |
| PUT | `/api/objects/{key}` | リクエスト本文をそのまま S3 に保存する(成功は 201) |
| GET | `/api/objects/{key}` | S3 から GET する(キーがなければ 404) |

キーに `/` を含むオブジェクトは、URL の設計上、扱えません。

## 設定

設定は `src/main/resources/application.yml` にあり、環境変数で変更できます。

| 環境変数 | 既定値 | 内容 |
|---|---|---|
| `AWS_REGION` | `ap-northeast-1` | リージョン |
| `S3_BUCKET` | `poc-bucket` | バケット名 |
| `S3_ENDPOINT` | (空) | エンドポイント。空なら実際の AWS に接続します。compose では `https://127.0.0.1:9297` |

認証情報は、AWS SDK の既定の探索順に従います。compose では、`entrypoint.sh` が
`AWS_SHARED_CREDENTIALS_FILE` にエミュレータが生成したファイルを指定します。

### リージョンについて

AWS Emulator にはリージョンを指定するオプションがありません。リージョンは、SDK / CLI 側の設定(このアプリでは `AWS_REGION`)で決まります。
エミュレータが生成する設定ファイル(`.emulator/9297/config`)の `region` は `us-east-1` ですが、アプリは環境変数を優先して使います。

## テスト

```sh
./gradlew test    # ローカルに Java 25 が必要。Docker で行う場合は下記
docker run --rm -v "$PWD:/w" -w /w gradle:9-jdk25 gradle test --no-daemon
```

`S3Service` をモックにした Spring MVC のテストです。エミュレータが必要な結合テストは、GitHub Actions の CI(`.github/workflows/ci.yml`)で
`docker compose` を起動し、PUT / GET / 404 を確認しています。

## テレメトリ

AWS Emulator は、使用状況のデータを AWS に送信する設定が既定で有効です。詳細は
[Telemetry in AWS Emulator](https://docs.aws.amazon.com/emulator/latest/userguide/telemetry.html) を参照してください。

この PoC の `compose.yaml` では、`emulator` の `command` に `--telemetry off` を指定して、**無効**にしています。

```yaml
command: ["-p", "9297", "--telemetry", "off"]
```

有効にしたい場合は、`command` を `["-p", "9297"]` に戻してください。

## 検証で分かったこと

- エミュレータの証明書は `localhost` / `127.0.0.1` / `::1` にしか対応していません。コンテナ間でサービス名や `host.docker.internal` で接続すると、ホスト名の不一致で SSL の検証が失敗します。
- エミュレータは `-p 0`(空きポートの自動選択)をコンテナ内で使えません(Docker がポートを転送できないため)。
- メモリは 2 GiB に制限しています。上限を超えるとエミュレータが終了し(終了コード 137)、エミュレートされたリソースは失われます。
- AWS CLI をローカルに入れていない場合は、`amazon/aws-cli` の Docker イメージを `--network container:<エミュレータのコンテナ>` で接続すると、`127.0.0.1:9297` で証明書の検証が通ります。

## ライセンス

[MIT License](LICENSE)

AWS Emulator は、このリポジトリには含まれません。AWS Emulator 本体のライセンスは、[aws/aws-emulator](https://github.com/aws/aws-emulator) を参照してください。
