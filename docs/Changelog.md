### Changelog

#### Nessus 1.0.0 Beta2

**Features**

* [#15][15] Provide nessus as a camel component 
* [#34][34] Add support for smart network fees
* [#54][54] Add JSON RPC API for IPFS content manager
* [#55][55] Add demo webapp as user interface
* [#56][56] Provide docker image for the bitcoin testnet
* [#59][59] Add a payment QR code for new addresses
* [#64][64] Allow to unregister address and IPFS content
* [#66][66] Allow to unregister address/IPFS through GUI
* [#67][67] Make rescan parameter dependent on blockchain config
* [#71][71] Allow better abstraction and branding of WebUI

For details see [1.0.0 Beta2 features](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta2"+label%3Afeature)

**Tasks**

* [#57][57] Setup and document VPS instance on BTC testnet
* [#61][61] Document a simple IPFS demo workflow
* [#68][68] Hide sendTo addresses that are not registered
* [#70][70] Prevent silent overwrite of plain content

For details see [1.0.0 Beta2 tasks](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta2"+label%3Atask)

**Bugs**

* [#60][60] IPFS get sets file handle to available=false
* [#62][62] Import of watch-only address silently ignored
* [#63][63] Address registration not seen on pruned instance
* [#65][65] Cannot spend single address registration UTXO

For details see [1.0.0 Beta2 bugs](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta2"+label%3Abug)

[15]: https://github.com/jboss-fuse/nessus/issues/15
[34]: https://github.com/jboss-fuse/nessus/issues/34
[54]: https://github.com/jboss-fuse/nessus/issues/54
[55]: https://github.com/jboss-fuse/nessus/issues/55
[56]: https://github.com/jboss-fuse/nessus/issues/56
[59]: https://github.com/jboss-fuse/nessus/issues/59
[64]: https://github.com/jboss-fuse/nessus/issues/64
[66]: https://github.com/jboss-fuse/nessus/issues/66
[67]: https://github.com/jboss-fuse/nessus/issues/67
[71]: https://github.com/jboss-fuse/nessus/issues/71
[57]: https://github.com/jboss-fuse/nessus/issues/57
[61]: https://github.com/jboss-fuse/nessus/issues/61
[68]: https://github.com/jboss-fuse/nessus/issues/68
[70]: https://github.com/jboss-fuse/nessus/issues/70
[60]: https://github.com/jboss-fuse/nessus/issues/60
[62]: https://github.com/jboss-fuse/nessus/issues/62
[63]: https://github.com/jboss-fuse/nessus/issues/63
[65]: https://github.com/jboss-fuse/nessus/issues/65

#### Nessus 1.0.0 Beta1

**Tasks**

* [#47][47] Investigate ipfs paths with whitespace
* [#53][53] Add generated changelog & release notes

For details see [1.0.0 Beta1 tasks](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta1"+label%3Atask)

**Bugs**

* [#41][41] Unnecessary delay when adding encrypted content
* [#42][42] Redeem change may transfer funds to another account

For details see [1.0.0 Beta1 bugs](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta1"+label%3Abug)

[47]: https://github.com/jboss-fuse/nessus/issues/47
[53]: https://github.com/jboss-fuse/nessus/issues/53
[41]: https://github.com/jboss-fuse/nessus/issues/41
[42]: https://github.com/jboss-fuse/nessus/issues/42

#### Nessus 1.0.0 Alpha5

**Tasks**

* [#50][50] Investigate find registration after wallet restart
* [#51][51] Verify that address registration survives wallet restart

For details see [1.0.0 Alpha5 tasks](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha5"+label%3Atask)

**Bugs**

* [#52][52] Stale IPFS content may never expire

For details see [1.0.0 Alpha5 bugs](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha5"+label%3Abug)

[50]: https://github.com/jboss-fuse/nessus/issues/50
[51]: https://github.com/jboss-fuse/nessus/issues/51
[52]: https://github.com/jboss-fuse/nessus/issues/52

#### Nessus 1.0.0 Alpha4

**Features**

* [#22][22] Provide a camel component for IPFS
* [#37][37] Add support for recursive IPFS add

For details see [1.0.0 Alpha4 features](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha4"+label%3Afeature)

**Tasks**

* [#23][23] Remove dependency on IPFS cmd line client
* [#39][39] Upgrade to ipfs-0.4.17
* [#44][44] Upgrade to ipfs-0.4.18
* [#45][45] Upgrade to undertow-2.0.15.Final
* [#46][46] Publish ipfs-0.4.18 docker image
* [#48][48] Revisit env var handling
* [#49][49] Rename Marry to Mary

For details see [1.0.0 Alpha4 tasks](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha4"+label%3Atask)

[22]: https://github.com/jboss-fuse/nessus/issues/22
[37]: https://github.com/jboss-fuse/nessus/issues/37
[23]: https://github.com/jboss-fuse/nessus/issues/23
[39]: https://github.com/jboss-fuse/nessus/issues/39
[44]: https://github.com/jboss-fuse/nessus/issues/44
[45]: https://github.com/jboss-fuse/nessus/issues/45
[46]: https://github.com/jboss-fuse/nessus/issues/46
[48]: https://github.com/jboss-fuse/nessus/issues/48
[49]: https://github.com/jboss-fuse/nessus/issues/49

#### Nessus 1.0.0 Alpha3

**Features**

* [#32][32] Add support for locked utxos
* [#33][33] IPFS ops from recorded blockchain UTOX must be asynchronous

For details see [1.0.0 Alpha3 features](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha3"+label%3Afeature)

[32]: https://github.com/jboss-fuse/nessus/issues/32
[33]: https://github.com/jboss-fuse/nessus/issues/33
