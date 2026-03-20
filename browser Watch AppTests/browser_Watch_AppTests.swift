//
//  browser_Watch_AppTests.swift
//  browser Watch AppTests
//
//  Created by Norembo on 20.03.26.
//

import Foundation
import Testing
@testable import browser_Watch_App

struct browser_Watch_AppTests {
    @Test func urlBuilderAddsHTTPSWhenMissing() async throws {
        let url = BrowserURLBuilder.makeURL(from: "apple.com")

        #expect(url?.absoluteString == "https://apple.com")
    }

    @Test func urlBuilderRejectsInvalidInput() async throws {
        let url = BrowserURLBuilder.makeURL(from: "not a valid host")

        #expect(url == nil)
    }

    @Test func urlBuilderCreatesReadableAddress() async throws {
        let url = try #require(URL(string: "https://www.apple.com/watch"))

        #expect(BrowserURLBuilder.displayAddress(for: url) == "www.apple.com/watch")
        #expect(BrowserURLBuilder.title(for: url) == "Apple")
    }

    @Test func storeRoundTripsFavorites() async throws {
        let favorites = [
            BrowserFavorite(
                title: "Apple",
                subtitle: "apple.com",
                icon: "apple.logo",
                url: try #require(URL(string: "https://apple.com"))
            )
        ]

        let encoded = BrowserStore.encodeFavorites(favorites)
        let decoded = BrowserStore.decodeFavorites(from: encoded)

        #expect(decoded == favorites)
    }

    @Test func relativeDateFormatterUsesMinutes() async throws {
        let referenceDate = Date(timeIntervalSince1970: 1_000)
        let now = Date(timeIntervalSince1970: 1_000 + 5 * 60)

        #expect(BrowserRelativeDateFormatter.string(from: referenceDate, now: now) == "5 min. pries")
    }

}
