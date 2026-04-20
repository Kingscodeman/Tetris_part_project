# 專案架構與 UML 設計 (OverviewUML.md)

本文件專注於 Tetris 專案的**整體靜態架構與類別關係**。如果想要了解專案是如何從零開始「一步一步」寫出來的，請參考 [`progression.md`](progression.md)。

---

## 1. 架構導向：類別責任分工

在物件導向開發中，將巨大的問題拆解成小零件是首要任務。我們的俄羅斯方塊專案將職責明確地劃分為以下四個核心類別。以下詳細列出目前的屬性（資料）與方法（行為）：

### 🖥️ `Tetris` (程式啟動與視窗容器)
* **職責**：專案的程式進入點。
* **核心方法**：
  * `main(String[] args)`：負責向作業系統要一個視窗 (`JFrame`)，設定視窗的標題、大小、關閉行為，最後將 `TetrisPanel` 放入視窗並顯示。

### 🕹️ `TetrisPanel` (遊戲引擎與顯示畫布)
* **繼承**：`extends JPanel`
* **職責**：遊戲的核心畫布，負責實際畫出背景與方塊。
* **主要屬性**：
  * `COLS`, `ROWS`, `CELL_SIZE`...：定義棋盤大小與座標的常數 (`static final`)。
  * `board` (`Color[][]`)：記錄 10x20 棋盤格顏色的二維陣列（目前於第一階段用來繪製空白背景格，尚未實作碰撞）。
  * `currentPiece` (`Tetromino`)：目前產生並準備在畫面上顯示的測試方塊。
* **核心方法**：
  * `TetrisPanel()` (建構子)：設定畫布大小、背景色，並呼叫 `randomPiece` 產生第一顆測試方塊。
  * `randomPiece()`：隨機挑選一個 `TetrominoType` 並回傳全新的 `Tetromino` 實體。
  * `paintComponent(Graphics g)`：Swing 繪製元件的核心方法。每次畫面需要更新時都會被 JVM 自動呼叫，負責驅動以下的繪圖方法。
  * `drawBoard()`, `drawCurrentPiece()`, `drawCell()`：將資料抽象邏輯轉換成實際畫面像素的方法。

### 🧊 `Tetromino` (動態方塊實體)
* **職責**：代表遊戲場上「活生生」的一個方塊。它純粹是一個輕量級的資料載體 (Data Object)。
* **主要屬性**：
  * `type` (`TetrominoType`)：這個方塊的本質類型（如 L 型、T 型）。
  * `rotation` (`int`)：目前的旋轉狀態索引（0-3）。
  * `x`, `y` (`int`)：方塊在 10x20 棋盤上的「相對格子座標」。
* **核心方法**：
  * `Tetromino(TetrominoType)` (建構子)：接收一個種類，並將各項數值（包含座標與旋轉）初始化為 0。

### 🧬 `TetrominoType` (靜態方塊藍圖)
* **型態**：`enum` (列舉)
* **職責**：定義俄羅斯方塊世界中所有的「原廠設定」。這是一組遊戲啟動後就不會改變的常數資料。
* **列舉實例**：`I`, `J`, `L`, `O`, `S`, `T`, `Z` 共 7 種。
* **主要屬性**：
  * `color` (`Color`)：該種類方塊的專屬顏色。
  * `shapes` (`int[][][]`)：儲存該方塊在 4 種旋轉角度下，組成該形狀的 4 個小格子的「相對座標偏移量」。
* **核心方法**：
  * 對應的建構子，用來在程式啟動瞬間，將形狀陣列與顏色綁定到上述 7 種實例身上。

---

## 2. UML 類別關係 (is-a / has-a)

要理解程式為何這樣寫，我們必須搞懂物件庫之間的依賴與繼承。

### 🔼 is-a 關係 (這是一種...)
通常表示**繼承 (Inheritance)** 或**實作 (Implementation)**，用來擴充原有類別的功能。
* **`TetrisPanel` is-a `JPanel`**
  * `TetrisPanel` 繼承了 Java Swing 內建的 `JPanel`。因為它「是一種」面板，所以它才能被加進視窗裡，而且才能覆寫畫畫的方法 (`paintComponent`)。
* **`TetrominoType` is-a `Enum`**
  * 在 Java 中，所有的 enum 都隱含繼承了 `java.lang.Enum`。

### 🔄 has-a 關係 (擁有...)
通常表示**組合 (Composition)** 或**聚合 (Aggregation)**，也就是「我的肚子裡裝了其他物件，我要靠它們來完成工作」。
* **`Tetris` has-a `TetrisPanel`**
  * 主程式擁有一個遊戲面板，把它放進視窗中。
* **`TetrisPanel` has-a `Tetromino`**
  * 面板上存在著一個用於測試顯示的靜態方塊 (`currentPiece`)。
* **`Tetromino` has-a `TetrominoType`**
  * 每個方塊實體，都會對應到一種「形狀種類」（例如：我這個正在落下的方塊，它的種類是 T型）。

---

## 3. 主要類別互動流程 (Sequence Overview)

在第一階段中，沒有動態邏輯（沒有計時器或鍵盤事件），以下是啟動視窗時的互動情境：

### 🎬 情境：遊戲啟動與初始化
1. OS 執行 `Tetris.main()`。
2. `Tetris` 建立 `JFrame` 視窗。
3. `Tetris` 建立 `TetrisPanel` 物件。
4. `TetrisPanel` 建構子內：
   * 向 `TetrominoType` 隨機抽出一種形狀。
   * 根據該形狀，建立一個新的 `Tetromino` 實體，作為初始測試方塊 (`currentPiece`)。
5. 視窗顯示，自動喚醒 `TetrisPanel.paintComponent()`，畫出背景格線與該初始測試方塊。
