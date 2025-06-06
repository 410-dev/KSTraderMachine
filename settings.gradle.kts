rootProject.name = "KSTraderMachine"

include("_sdk:Foundation")
findProject(":_sdk:Foundation")?.name = "Foundation"
include("_sdk:Graphite")
findProject(":_sdk:Graphite")?.name = "Graphite"
include("_sdk:liblks")
findProject(":_sdk:liblks")?.name = "liblks"
include("_sdk:KSTraderAPI")
findProject(":_sdk:KSTraderAPI")?.name = "KSTraderAPI"
include("_sdk:KSSuite")
findProject(":_sdk:KSSuite")?.name = "KSSuite"
include("_drivers:UpBit")
findProject(":_drivers:UpBit")?.name = "UpBit"
include("strategies:GeneralV1")
findProject(":strategies:GeneralV1")?.name = "GeneralV1"
include("_drivers:UpBit")
findProject(":_drivers:UpBit")?.name = "UpBit"
include("_dStrategies:GeneralV1OverWS")
findProject(":_dStrategies:GeneralV1OverWS")?.name = "GeneralV1OverWS"
include("_drivers:Binance")
findProject(":_drivers:Binance")?.name = "Binance"
include("_services:KSNotificationServer")
findProject(":_services:KSNotificationServer")?.name = "KSNotificationServer"
include("_services:DiscordUIServer")
findProject(":_services:DiscordUIServer")?.name = "DiscordUIServer"
include("_sdk:KSSocket")
findProject(":_sdk:KSSocket")?.name = "KSSocket"
include("_services:Diagnostics")
findProject(":_services:Diagnostics")?.name = "Diagnostics"
include("_services:UpgradeManager")
findProject(":_services:UpgradeManager")?.name = "UpgradeManager"
include("_servers:ActivationServer")
findProject(":_servers:ActivationServer")?.name = "ActivationServer"
include("_services:ServicesControlService")
findProject(":_services:ServicesControlService")?.name = "ServicesControlService"
include("Applications:KSManualTrade")
findProject(":Applications:KSManualTrade")?.name = "KSManualTrade"
include("Applications:KSTraderSuiteInstaller")
findProject(":Applications:KSTraderSuiteInstaller")?.name = "KSTraderSuiteInstaller"
