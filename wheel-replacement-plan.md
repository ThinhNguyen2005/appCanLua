# Káº¿ hoáº¡ch thay tháº¿ code tá»± triá»ƒn khai

## Má»¥c tiĂªu

Giáº£m code trĂ¹ng láº·p vĂ  code ná»n táº£ng tá»± triá»ƒn khai, Ä‘á»“ng thá»i giá»¯ tÆ°Æ¡ng thĂ­ch dá»¯ liá»‡u, UI vĂ  hĂ nh vi hiá»‡n táº¡i cá»§a CĂ¢n LĂºa.

## CĂ´ng viá»‡c

- [x] Thay JSON tombstone thá»§ cĂ´ng báº±ng `JSONObject`, giá»¯ schema cÅ© vĂ  thĂªm test control characters.
- [x] Thay AES-CBC hardcoded báº±ng AES-GCM Android Keystore cĂ³ Ä‘á»‹nh dáº¡ng version vĂ  Ä‘á»c Ä‘Æ°á»£c dá»¯ liá»‡u cÅ©.
- [x] XĂ³a `TtsHelper`, sá»­a lifecycle TTS, giáº£i phĂ³ng audio resource vĂ  khai bĂ¡o TTS service query.
- [x] DĂ¹ng `LocalHapticFeedback` cho pháº£n há»“i chuáº©n trong Compose, giá»¯ wrapper nhá» cho callback TTS.
- [x] XĂ³a `HtmlUtils` cĂ¹ng test vĂ¬ khĂ´ng cĂ²n production caller.
- [x] Thay biá»ƒu Ä‘á»“ cá»™t vĂ  donut Canvas báº±ng Vico 3.2.2, giá»¯ card, toggle, mĂ u, nhĂ£n vĂ  bá»‘n mĂ¹a vá»¥.
- [x] Cáº­p nháº­t backup rules phĂ¹ há»£p vá»›i khĂ³a Keystore.
- [x] Unit tests va compileDebugKotlin da thanh cong; lint toan repo con loi ton tai truoc thay doi.

## HoĂ n táº¥t khi

- [x] Dá»¯ liá»‡u CCCD cÅ© váº«n Ä‘á»c Ä‘Æ°á»£c vĂ  dá»¯ liá»‡u má»›i khĂ´ng dĂ¹ng khĂ³a/IV hardcoded.
- [x] Tombstone khĂ´i phá»¥c Ä‘Æ°á»£c, khĂ´ng lĂ m lá»™ CCCD dáº¡ng rĂµ.
- [x] Bieu do so sanh mua vu dung Vico, khong con Canvas chart tu ve.
- [x] Build va test lien quan thanh cong; lint toan repo con loi khong thuoc thay doi.

## LÆ°u Ă½

CĂ¡c thay Ä‘á»•i UI chÆ°a commit cá»§a ngÆ°á»i dĂ¹ng Ä‘Æ°á»£c giá»¯ nguyĂªn vá» bá»‘ cá»¥c vĂ  hĂ nh vi; viá»‡c chuyá»ƒn Vico chá»‰ thay engine render biá»ƒu Ä‘á»“.
