# payroll-system

## プロジェクト概要

Kotlin と Gradle を用いて実装された CLI ベースの給与システムです。従業員の追加・削除、タイムカードの記録、売上伝票の登録、サービス料や組合費の適用、支払い処理（給料日処理）を行います。
Exposed ORM を用いて PostgreSQL にデータを永続化します。

## 必要環境
- JDK 21 以上
- Docker & Docker Compose
- (任意) [just](https://github.com/casey/just)（タスクランナー）

## 初期設定
1. リポジトリをクローン
   ```bash
   git clone <リポジトリURL>
   cd payroll-system
   ```
2. Docker コンテナ起動
   ```bash
   docker-compose up -d
   ```
3. データベース初期化
   ```bash
   # Gradle タスクで初期化
   ./gradlew dbInit
   # or just を利用する場合
   just up
   ```
4. ビルド
   ```bash
   ./gradlew build
   ```

## 実行方法

### Gradle Wrapper を使う場合
```bash
./gradlew run --args="<サブコマンド> <オプション>"
```

### bin スクリプトを使う場合
```bash
bin/sala <サブコマンド> <オプション>
```

### just を使う場合
```bash
# Docker 起動 + DB 初期化 + ビルド + 実行
just up
# コンテナ停止・削除
just down
```

## サブコマンド一覧
- AddEmp: 従業員追加
  ```bash
  bin/sala AddEmp -e 1 -n 太郎 -a 東京 H 1500
  ```
- DeleteEmp: 従業員削除
  ```bash
  bin/sala DeleteEmp -id 1
  ```
- TimeCard: タイムカード登録
  ```bash
  bin/sala TimeCard -id 1 -d 2024-06-17 -h 8
  ```
- SalesReceipt: 売上伝票登録
  ```bash
  bin/sala SalesReceipt -id 1 -d 2024-06-17 -s 50000
  ```
- ServiceCharge: サービス料金登録
  ```bash
  bin/sala ServiceCharge -id 1 -s 100.0
  ```
- ChangeEmployee: 従業員情報変更
  ```bash
  bin/sala ChangeEmployee -id 1 Address 新宿
  ```
- Payday: 給料日処理
  ```bash
  bin/sala Payday -d 2024-06-30
  ```

## テスト
```bash
./gradlew test
```

## 環境変数
- DATABASE_URL: 接続先データベースのURL（デフォルト: `postgresql://postgres:postgres@localhost:5432/postgres`）

## 使用ライブラリ
- Kotlinx CLI (`kotlinx-cli`)
- Exposed ORM (`org.jetbrains.exposed`)
- PostgreSQL ドライバ
- H2 ドライバ
- Guava
