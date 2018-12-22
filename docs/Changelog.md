### Changelog

#### Nessus 1.0.0 Beta3

**Features**

* [#30][30] Add abstractions for blockchain, network and wallet
* [#36][36] Make encrypted IPFS content deterministic
* [#69][69] Add support for directory upload
* [#83][83] Replace file lists with tree views
* [#85][85] Allow docker process to access plain files on host
* [#87][87] Allow data directory to be customized
* [#93][93] Intelligent caching for address/ipfs utxos
* [#94][94] Add RPC call to find registered addresses at once
* [#95][95] Add support for registered user meta data
* [#100][100] Add support for config through cmd line opts
* [#104][104] Make encryption strength configurable
* [#105][105] Make address registration idempotent
* [#107][107] Make JAXRS context path configurable
* [#111][111] Provide a data only blockstore image
* [#112][112] Show blockcount in webui footer
* [#113][113] Add support for cmd line --help option
* [#121][121] Provide direct IPFS access in UI layer

For details see [1.0.0 Beta3 features](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta3"+label%3Afeature)

**Tasks**

* [#44][44] Upgrade to ipfs-0.4.18
* [#73][73] Remove dependency on BitcoinAddress in higher layers
* [#74][74] Build Docker images as part of the maven build
* [#75][75] Upgrade to bitcoin-rpc-client-1.1.0
* [#76][76] Add some version info to the GUI
* [#77][77] Report root path on file system
* [#84][84] Improve documentation of the JAXRS API
* [#88][88] Make local content override configurable
* [#89][89] Migrate project to jboss-fuse
* [#90][90] Prevent empty json properties in SFHandle
* [#98][98] Migrate IPFS ids from String to Multihash
* [#102][102] Check if we can delegate to SecureRandom with seed
* [#103][103] Add meaningful header to address registration
* [#110][110] Migrate from account to label
* [#117][117] Document encryption/decryption workflow
* [#119][119] Add IPFS connect info to WebUI footer

For details see [1.0.0 Beta3 tasks](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta3"+label%3Atask)

**Bugs**

* [#27][27] Multiple clients may see different address labels 
* [#28][28] Unavailable IPFS content should not cause errors
* [#31][31] sendFromLabel may attempt to send negative amount
* [#40][40] API may show incorrect balance
* [#43][43] IllegalStateException regarding the response channel during a web request
* [#79][79] Removing last plain file does not remove parent dir
* [#80][80] jaxrs and webui may fail to start
* [#81][81] bitcoind takes much too long to load blockstore
* [#82][82] QR code may not display properly
* [#86][86] User errors not displayed on error handling page
* [#91][91] Find IPFS content returns handle with encrypted=false
* [#92][92] Cannot unregister multiple content ids
* [#101][101] Find address registration may hang forever
* [#106][106] Insufficient funds running the jaxrs tests alone
* [#108][108] Request on invalid root path returns nothing
* [#114][114] Get operation on IPFS directory not disabled
* [#115][115] Unregister address does not remove ipfs content
* [#116][116] Cannot add local content repeatedly
* [#118][118] Multiple threads may fetch same content id
* [#124][124] NPE in error page may result in empty screen
* [#125][125] Sending content may assign incorrect owner
* [#126][126] Threads may try to get already removed content

For details see [1.0.0 Beta3 bugs](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Beta3"+label%3Abug)

[30]: https://github.com/jboss-fuse/nessus/issues/30
[36]: https://github.com/jboss-fuse/nessus/issues/36
[69]: https://github.com/jboss-fuse/nessus/issues/69
[83]: https://github.com/jboss-fuse/nessus/issues/83
[85]: https://github.com/jboss-fuse/nessus/issues/85
[87]: https://github.com/jboss-fuse/nessus/issues/87
[93]: https://github.com/jboss-fuse/nessus/issues/93
[94]: https://github.com/jboss-fuse/nessus/issues/94
[95]: https://github.com/jboss-fuse/nessus/issues/95
[100]: https://github.com/jboss-fuse/nessus/issues/100
[104]: https://github.com/jboss-fuse/nessus/issues/104
[105]: https://github.com/jboss-fuse/nessus/issues/105
[107]: https://github.com/jboss-fuse/nessus/issues/107
[111]: https://github.com/jboss-fuse/nessus/issues/111
[112]: https://github.com/jboss-fuse/nessus/issues/112
[113]: https://github.com/jboss-fuse/nessus/issues/113
[121]: https://github.com/jboss-fuse/nessus/issues/121
[44]: https://github.com/jboss-fuse/nessus/issues/44
[73]: https://github.com/jboss-fuse/nessus/issues/73
[74]: https://github.com/jboss-fuse/nessus/issues/74
[75]: https://github.com/jboss-fuse/nessus/issues/75
[76]: https://github.com/jboss-fuse/nessus/issues/76
[77]: https://github.com/jboss-fuse/nessus/issues/77
[84]: https://github.com/jboss-fuse/nessus/issues/84
[88]: https://github.com/jboss-fuse/nessus/issues/88
[89]: https://github.com/jboss-fuse/nessus/issues/89
[90]: https://github.com/jboss-fuse/nessus/issues/90
[98]: https://github.com/jboss-fuse/nessus/issues/98
[102]: https://github.com/jboss-fuse/nessus/issues/102
[103]: https://github.com/jboss-fuse/nessus/issues/103
[110]: https://github.com/jboss-fuse/nessus/issues/110
[117]: https://github.com/jboss-fuse/nessus/issues/117
[119]: https://github.com/jboss-fuse/nessus/issues/119
[27]: https://github.com/jboss-fuse/nessus/issues/27
[28]: https://github.com/jboss-fuse/nessus/issues/28
[31]: https://github.com/jboss-fuse/nessus/issues/31
[40]: https://github.com/jboss-fuse/nessus/issues/40
[43]: https://github.com/jboss-fuse/nessus/issues/43
[79]: https://github.com/jboss-fuse/nessus/issues/79
[80]: https://github.com/jboss-fuse/nessus/issues/80
[81]: https://github.com/jboss-fuse/nessus/issues/81
[82]: https://github.com/jboss-fuse/nessus/issues/82
[86]: https://github.com/jboss-fuse/nessus/issues/86
[91]: https://github.com/jboss-fuse/nessus/issues/91
[92]: https://github.com/jboss-fuse/nessus/issues/92
[101]: https://github.com/jboss-fuse/nessus/issues/101
[106]: https://github.com/jboss-fuse/nessus/issues/106
[108]: https://github.com/jboss-fuse/nessus/issues/108
[114]: https://github.com/jboss-fuse/nessus/issues/114
[115]: https://github.com/jboss-fuse/nessus/issues/115
[116]: https://github.com/jboss-fuse/nessus/issues/116
[118]: https://github.com/jboss-fuse/nessus/issues/118
[124]: https://github.com/jboss-fuse/nessus/issues/124
[125]: https://github.com/jboss-fuse/nessus/issues/125
[126]: https://github.com/jboss-fuse/nessus/issues/126

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
* [#45][45] Upgrade to undertow-2.0.15.Final
* [#46][46] Publish ipfs-0.4.18 docker image
* [#48][48] Revisit env var handling
* [#49][49] Rename Marry to Mary

For details see [1.0.0 Alpha4 tasks](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha4"+label%3Atask)

[22]: https://github.com/jboss-fuse/nessus/issues/22
[37]: https://github.com/jboss-fuse/nessus/issues/37
[23]: https://github.com/jboss-fuse/nessus/issues/23
[39]: https://github.com/jboss-fuse/nessus/issues/39
[45]: https://github.com/jboss-fuse/nessus/issues/45
[46]: https://github.com/jboss-fuse/nessus/issues/46
[48]: https://github.com/jboss-fuse/nessus/issues/48
[49]: https://github.com/jboss-fuse/nessus/issues/49

#### Nessus 1.0.0 Alpha3

**Features**

* [#1][1] Add initial bitcoin wallet operations
* [#3][3] Add support for getting a new address
* [#4][4] Send bitcoin to address
* [#5][5] Create simple raw transaction
* [#6][6] Complex raw transaction
* [#8][8] Add support for Bitcoin ImportMulti
* [#10][10] Fluent API to work with non-trivial transactions
* [#14][14] Add support for OP_RETURN data
* [#19][19] Add support for blocks API
* [#25][25] Add initial encrypted IPFS workflow
* [#26][26] Add support for local IPFS content
* [#32][32] Add support for locked utxos
* [#33][33] IPFS ops from recorded blockchain UTOX must be asynchronous

For details see [1.0.0 Alpha3 features](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha3"+label%3Afeature)

**Tasks**

* [#2][2] Generate bitcoin for regtest network
* [#9][9] Migrate wallet to bag of keys
* [#11][11] Add demo for a simple payment system
* [#13][13] Encrypt arbitrary data with an encryption token
* [#18][18] Replace the use of double with BigDecimal
* [#20][20] Add a simple IPFS client
* [#21][21] Update project license
* [#24][24] Allow get balance with null label 
* [#29][29] Explicitly lock recoded data UTOX

For details see [1.0.0 Alpha3 tasks](https://github.com/jboss-fuse/nessus/issues?q=milestone%3A"1.0.0+Alpha3"+label%3Atask)

[1]: https://github.com/jboss-fuse/nessus/issues/1
[3]: https://github.com/jboss-fuse/nessus/issues/3
[4]: https://github.com/jboss-fuse/nessus/issues/4
[5]: https://github.com/jboss-fuse/nessus/issues/5
[6]: https://github.com/jboss-fuse/nessus/issues/6
[8]: https://github.com/jboss-fuse/nessus/issues/8
[10]: https://github.com/jboss-fuse/nessus/issues/10
[14]: https://github.com/jboss-fuse/nessus/issues/14
[19]: https://github.com/jboss-fuse/nessus/issues/19
[25]: https://github.com/jboss-fuse/nessus/issues/25
[26]: https://github.com/jboss-fuse/nessus/issues/26
[32]: https://github.com/jboss-fuse/nessus/issues/32
[33]: https://github.com/jboss-fuse/nessus/issues/33
[2]: https://github.com/jboss-fuse/nessus/issues/2
[9]: https://github.com/jboss-fuse/nessus/issues/9
[11]: https://github.com/jboss-fuse/nessus/issues/11
[13]: https://github.com/jboss-fuse/nessus/issues/13
[18]: https://github.com/jboss-fuse/nessus/issues/18
[20]: https://github.com/jboss-fuse/nessus/issues/20
[21]: https://github.com/jboss-fuse/nessus/issues/21
[24]: https://github.com/jboss-fuse/nessus/issues/24
[29]: https://github.com/jboss-fuse/nessus/issues/29