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
* **職責**：遊戲的核心控制中心，掌握所有繪圖、輸入與遊戲規則（碰撞、消去、計分、遊戲狀態）。
* **主要屬性**：
  * `COLS`, `ROWS`, `CELL_SIZE`...：定義棋盤大小與座標的常數 (`static final`)。
  * `board` (`Color[][]`)：記錄 10x20 棋盤格顏色的二維陣列，儲存已到底部固定的死方塊。
  * `currentPiece`, `nextPiece` (`Tetromino`)：目前正在操控並準備掉落的方塊，以及預告的下一個方塊。
  * `timer` (`Timer`)：遊戲的心跳，定期觸發方塊下落。
  * `score`, `linesCleared`, `level` (`int`)：記錄玩家目前的分數、消除行數與等級難度。
  * `isGameOver`, `isPaused` (`boolean`)：控制遊戲流程的狀態機旗標。
* **核心方法**：
  * `TetrisPanel()` (建構子)：初始化畫布，設定鍵盤事件 (`setupKeyBindings`)，並啟動 `Timer`。
  * `gameStep()`：每次 `Timer` 時間到時執行，負責處理自然下落、碰撞與鎖定。執行前會檢查 `isGameOver` 與 `isPaused`。
  * `isValidPosition()`：核心邏輯！透過陣列運算判斷方塊未來的座標是否撞牆或撞到 `board` 中的方塊。
  * `moveCurrentPiece()`, `rotateCurrentPiece()`：處理方塊位移與旋轉，依賴 `isValidPosition` 來決定是否放行。
  * `lockCurrentPiece()`, `clearCompletedLines()`：方塊觸底時將顏色寫入 `board`，並檢查是否可以消除滿排橫列並計分。
  * `paintComponent(Graphics g)` 與其他 `draw...` 方法：負責將狀態轉換成實際畫面像素（包含繪製背景、固化方塊、當前方塊、Ghost Piece 影子與 UI 文字等）。

### 🎵 `SoundManager` (音效控制器 - 規劃中)
* **職責**：收納前面開發的 MIDI 音效合成器邏輯，專司發出遊戲內的各種音響。
* **核心方法**：提供 `playTone(...)` 等方法給 `TetrisPanel` 於碰撞、消除或結束時呼叫。

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
  * 主程式擁有一個遊戲面板，把它放進視窗中。主視窗未來也可能擁有 `SidePanel` 來顯示分數。
* **`TetrisPanel` has-a `Tetromino` (多個)**
  * 面板上動態記錄著目前正在掉落的方塊 (`currentPiece`)，以及下一個將會產生的預告方塊 (`nextPiece`)。
* **`TetrisPanel` has-a `Timer`**
  * 面板擁有一個計時器，當作遊戲運作的心跳。
* **`TetrisPanel` has-a `Color[][]`**
  * 面板擁有一個二維顏色陣列，作為「死掉固化的積塊們」的永久記憶體。
* **`TetrisPanel` has-a `SoundManager`**
  * 面板擁有音效管理員，在發生特定事件(如消除、結束)時呼叫其發聲。
* **`Tetromino` has-a `TetrominoType`**
  * 每個方塊實體，都會對應到一種「形狀種類」（例如：我這個正在落下的方塊，它的種類是 T型）。

---

## 3. 主要類別互動流程 (Sequence Overview)

目前專案已經具備完整的動態邏輯，包含輸入、碰撞與遊戲迴圈。以下是執行時的三個最核心互動情境：

### 🎬 情境一：遊戲啟動與初始化
1. OS 執行 `Tetris.main()`。
2. `Tetris` 建立 `JFrame` 視窗與 `TetrisPanel` 物件。
3. `TetrisPanel` 建構子內：
   * 設定鍵盤綁定 (`setupKeyBindings`)。
   * 初始化新的 `Tetromino` 作為初始下落方塊 (`currentPiece`)。
   * 啟動 `Timer` 定時器來推動遊戲迴圈。
4. 視窗顯示，自動喚醒 `TetrisPanel.paintComponent()` 畫出初始畫面。

### ⏳ 情境二：自然下落 (Timer 觸發)
1. `Timer` 時間到，觸發 `TetrisPanel.gameStep()`。
2. 系統首先會檢查遊戲狀態旗標：若 `isGameOver` 或 `isPaused` 為真，則立刻提早結束 (return)。
3. 嘗試將 `currentPiece` 的 `y` 座標 +1，並利用 `isValidPosition()` 往未來的位移撞擊做檢查。
4. **若無碰撞**：更新方塊座標，呼叫 `repaint()` 重繪畫面。
5. **若發生碰撞 (觸底)**：
   * 觸發鎖定音效 (`SoundManager`)。
   * 呼叫 `lockCurrentPiece()`，把方塊的顏色寫進 `board` 二維陣列。
   * 呼叫 `clearCompletedLines()`，掃描 `board` 消去滿橫列並累加分數；若有消除會播放消除音效。
   * 將 `nextPiece` 取代作為新的 `currentPiece`，並立刻呼叫 `isValidPosition()` 檢查是否重生就立刻撞牆。
   * 若重生即撞牆，設置 `isGameOver = true` 並觸發遊戲結束音效；反之則產生一個全新的 `nextPiece`，繼續下一局迴圈。

### 🎹 情境三：玩家輸入 (鍵盤操作)
1. 玩家按下「左右方向鍵」或「上方旋轉鍵」。
2. Java Swing 系統攔截按鍵，觸發 `ActionMap` 中預先綁定的對應方法（如 `moveCurrentPiece` / `rotateCurrentPiece`）。
3. 系統計算出方塊**預期的未來座標或變更後的旋轉角度** (如果遊戲是暫停或結束狀態，系統會在第一步直接阻擋計算)。
4. 系統將這些未來資訊交給 `isValidPosition()` 檢驗 (確保沒撞到現有牆壁與固化方塊)。
5. 檢查若為合法，才真正變更 `currentPiece` 中的內部資料，並通知 OS `repaint()` 執行畫面重繪。
