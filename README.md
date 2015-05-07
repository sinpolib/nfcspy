# nfcspy
NFC Spy is an Android app, It can watch APDUs transceived between reader and contactless smart card.

The latest version will be published in https://play.google.com/store/apps/details?id=com.sinpo.nfcspy

---------------------------------------------------------------------------

This is useful for NFC/HCE developer to learn about the underlying communication protocol, debugging equential error, etc. However, you should NOT use it for illegal acts, or you will be solely responsible for any consequences thereof. In addition, this app may be need to root/modify your system to work better, please be careful when doing this, and again, Developer of this app do NOT accept any responsibility.

To use this app, You need two android phones both has NFC hardware, and at least one is runing 4.4 or greater version for HCE suport.

One phone act as a emulated card using NFC card emulation mode (HCE, starting from android 4.4 kitKat).

Another phone act as a card reader using NFC reader mode.

This two phones connected to each other using WLAN direct (WiFiP2P).

When first phone close to a REAL card reader (POS, ATM, etc.), it will send all APDUs it received to the second phone by WiFi?-P2P socket. The second phone gets APDUs, and then send these to a REAL card which attached close to it, similarly, the second phone send respones APDUs from REAL card to first phone, then the REAL card reader.

Eventually the REAL reader will act as read a REAL card directly, and this app will record all APDUs sent and received, that is how this app works, and why call it 'NFC Spy'.

NOTE:

If you also installed other HCE apps, you may see multiple items in the 'Tap & Pay' page of System Settings, And you need select NFC Spy's card before use it.

Android's HCE architecture use HCE service to implement card emulation, every service need to declare AIDs it will respone when card reader select application by aid. This leads to three problems, first, only ISO14443-A/ISO7816 compatible smart card can be emulated, second is NFC Spy can only handle limited card type, the last is HCE only works with card reader which send a stardard ISO7816 SELECT NAME/AID command as the first APDU.

The first problem can NOT be resolved for the time being, unless you switch to other implemention of card emulation such as some versions of CyanogenMod?, but what will be another story. To solve the second, you can use a rooted phone with Xposed framework, add NFC Spy or 'NFC Card-Emulation Catch-All Routing' mod. To solve the last one, you may need a custom ROM which has modified system libaray.
