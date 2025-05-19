rootProject.name = "KSTraderMachine"
include("_sdk:Foundation")
findProject(":_sdk:Foundation")?.name = "Foundation"
include("_sdk:Graphite")
findProject(":_sdk:Graphite")?.name = "Graphite"
include("_sdk:liblks")
findProject(":_sdk:liblks")?.name = "liblks"
include("_sdk:KSTraderAPI")
findProject(":_sdk:KSTraderAPI")?.name = "KSTraderAPI"
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
include("OtherApps:KSManualTrader")
findProject(":OtherApps:KSManualTrader")?.name = "KSManualTrader"
include("OtherApps:KSNotificationServer")
findProject(":OtherApps:KSNotificationServer")?.name = "KSNotificationServer"
