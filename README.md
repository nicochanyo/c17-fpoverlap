# C17 鎖屏流體雲優化 (C17 Lock & Fluid Cloud)

專為 **ColorOS 17**（基於 LibXposed API 102）打造的系統介面優化模組，整合短焦指紋膠囊對齊與自動隱藏、音樂鎖屏指紋共存，以及靈動島與流體雲的外觀、高光、遮罩調節功能。

- **包名**：`io.github.nicochanyo.c17fpoverlap`
- **建議作用域**：`com.android.systemui`（系統介面）
- **適用系統**：ColorOS 17（Android 17）
- **Xposed API**：LibXposed API 102
- **開源協議**：GPL-3.0（詳見 [LICENSE](LICENSE)）

---

## 主要功能

### 鎖屏與指紋
- **膠囊對齊指紋**：將鎖屏與全屏 AOD 膠囊下移至短焦指紋位置，消除從桌面進鎖屏的跳動與 AOD 上抬問題。
- **重疊時隱藏指紋圖標**：膠囊覆蓋指紋區域時自動淡隱指紋圖標，不影響按壓解鎖。
- **垂直位置微調**：支援 `-150 ~ +150 px` 精細調整膠囊高度。
- **音樂鎖屏顯示指紋**：展開鎖屏媒體卡時保留指紋圖標與按壓解鎖。
- **AOD 通知半透明毛玻璃**：全屏 AOD 下移除一般通知黑底，呈現與媒體卡一致的半透明毛玻璃效果。
- **通知高光跟隨媒體**：鎖屏一般通知（含堆疊卡片）邊框高光跟隨當前音樂封面色。

### 流體雲與靈動島外觀
- **透明度與模糊**：獨立調節頂部展開卡片與鎖屏流體雲不透明度（`0% ~ 100%`）及模糊強度（`0% ~ 300%`）。
- **高光調節**：大媒體卡片與單行流體雲卡片分別支援高光亮度／彩度雙向增強（`0% ~ 200%`）。
- **遮罩設定**：
  - 頂部展開卡片、鎖屏與下拉通知列黑色漸變遮罩三處獨立開關。
  - 未移除遮罩的區塊支援跟隨音樂封面自動變色，並可獨立調整遮罩亮度、彩度與濃度。
  - 支援非媒體卡片遮罩／高光跟隨媒體變色或自訂固定顏色。
- **卡片底色**：支援維持系統預設或自訂 HEX 色碼。

---

## 简体中文说明

专为 **ColorOS 17**（基于 LibXposed API 102）设计的系统界面优化模块，整合短焦指纹胶囊对齐与自动隐藏、音乐锁屏指纹共存，以及灵动岛和流体云透明度、模糊、高光与遮罩调节功能。

- **包名**：`io.github.nicochanyo.c17fpoverlap`
- **推荐作用域**：`com.android.systemui`（系统界面）
- **适用系统**：ColorOS 17（Android 17）
- **使用提示**：请在 LSPosed 中仅勾选「系统界面」，启用后重启系统界面即可生效；切勿与其他流体云调节或指纹重叠模块同时启用。

---

## English

A **ColorOS 17** Xposed module (LibXposed API 102) combining optical fingerprint capsule alignment & auto-hide, music lockscreen fingerprint coexistence, and Dynamic Island / Fluid Cloud appearance controls.

- **Package Name**: `io.github.nicochanyo.c17fpoverlap`
- **Scope**: `com.android.systemui` (System UI)
- **Requirements**: ColorOS 17 (Android 17), LSPosed with API 102 support

### Features
- **Optical FP & Capsule**: Align lockscreen & AOD capsule onto the fingerprint sensor without jump/lift, auto-fade FP icon on overlap while keeping unlock active, and fine-tune vertical offset (`-150 ~ +150 px`).
- **Music Lockscreen & Notifications**: Keep FP unlock under expanded lockscreen media cards, enable translucent glass AOD notifications, and tint lockscreen notification border highlights from album art.
- **Fluid Cloud & Dynamic Island Appearance**:
  - Independent opacity sliders for top expanded cards & lockscreen pills plus blur strength (`0% ~ 300%`).
  - Independent highlight brightness & chroma (`0% ~ 200%`) for large media cards and single-line cards.
  - Independent black gradient mask toggles for expanded card, lockscreen, and shade, plus album-art tinting with dedicated brightness, chroma, and opacity controls.

---

## 鳴謝 / Credits
- 短焦指紋鎖屏重疊原作者：酷安 `@白无爱`
- 流體雲調節原作者：酷安 `@天伞桜` (`com.sakur.fluidcloud`)
- 二改與維護：`o永夜o` (`@nicochanyo`)

> 本模組於 GitHub 與 LSPosed 倉庫完全免費開源提供，請勿在任何第三方平台或群組付費購買，謹防受騙。

---

## 開源協議 / License

本專案採用 **GNU General Public License v3.0 (GPL-3.0)** 開源協議，完整條款請參閱 [LICENSE](LICENSE)。

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** — see the [LICENSE](LICENSE) file for details.
