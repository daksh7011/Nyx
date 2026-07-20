import SwiftUI

@main
struct NyxApp: App {
    init() {
        KoinInitKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
        }
    }
}
