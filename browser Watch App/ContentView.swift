//
//  ContentView.swift
//  browser Watch App
//
//  Created by Norembo on 20.03.26.
//

import Foundation
import SwiftUI

struct ContentView: View {
    @Environment(\.openURL) private var openURL

    @AppStorage("savedFavorites") private var savedFavoritesData = ""
    @AppStorage("savedHistory") private var savedHistoryData = ""

    @State private var address = "apple.com"
    @State private var statusMessage = "Ivesk adresa ir atidaryk puslapi."
    @State private var favorites: [BrowserFavorite] = []
    @State private var history: [BrowserHistoryEntry] = []

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                heroCard
                composerCard

                if !favorites.isEmpty {
                    sectionCard(title: "Megstamos", systemImage: "star.fill") {
                        ForEach(favorites) { favorite in
                            BrowserRowButton(
                                title: favorite.title,
                                subtitle: favorite.displayAddress,
                                icon: favorite.icon
                            ) {
                                open(favorite.url)
                            }
                        }
                    }
                }

                if !history.isEmpty {
                    sectionCard(title: "Istorija", systemImage: "clock.arrow.circlepath") {
                        ForEach(history) { entry in
                            BrowserRowButton(
                                title: entry.title,
                                subtitle: entry.subtitle,
                                icon: "clock"
                            ) {
                                open(entry.url)
                            }
                        }

                        Button("Isvalyti istorija", role: .destructive) {
                            clearHistory()
                        }
                        .font(.footnote.weight(.semibold))
                    }
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
        }
        .background(backgroundGradient)
        .navigationTitle("Browser")
        .onAppear {
            loadStoredData()
        }
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "safari.fill")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.white)
                    .frame(width: 32, height: 32)
                    .background(
                        Circle()
                            .fill(.blue.gradient)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    Text("Apple Watch Browser")
                        .font(.headline)
                    Text("Greitas paleidimas, megstamos svetaines ir istorija.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }

            Text(statusMessage)
                .font(.footnote.weight(.medium))
                .foregroundStyle(.blue)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(cardBackground)
    }

    private var composerCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Adresas")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)

            TextField("apple.com", text: $address)
                .textInputAutocapitalization(.never)
                .disableAutocorrection(true)

            if let previewURL = normalizedAddressURL {
                Text(previewURL.absoluteString)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }

            HStack(spacing: 8) {
                Button {
                    openCurrentAddress()
                } label: {
                    Label("Atidaryti", systemImage: "arrow.up.right.circle.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(.blue)
                .disabled(normalizedAddressURL == nil)

                Button {
                    toggleFavoriteForCurrentAddress()
                } label: {
                    Image(systemName: isCurrentAddressFavorite ? "star.fill" : "star")
                        .frame(width: 34, height: 34)
                }
                .buttonStyle(.bordered)
                .tint(.yellow)
                .disabled(normalizedAddressURL == nil)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(cardBackground)
    }

    private var backgroundGradient: some View {
        LinearGradient(
            colors: [
                Color(red: 0.05, green: 0.08, blue: 0.16),
                Color(red: 0.02, green: 0.02, blue: 0.05)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }

    private var cardBackground: some ShapeStyle {
        .thinMaterial
    }

    private var normalizedAddressURL: URL? {
        BrowserURLBuilder.makeURL(from: address)
    }

    private var isCurrentAddressFavorite: Bool {
        guard let url = normalizedAddressURL else { return false }
        return favorites.contains { $0.url == url }
    }

    @ViewBuilder
    private func sectionCard<Content: View>(
        title: String,
        systemImage: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Label(title, systemImage: systemImage)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)

            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(cardBackground)
    }

    private func loadStoredData() {
        favorites = BrowserStore.decodeFavorites(from: savedFavoritesData) ?? BrowserFavorite.samples
        history = BrowserStore.decodeHistory(from: savedHistoryData) ?? []
        persistIfNeeded()
    }

    private func openCurrentAddress() {
        guard let url = normalizedAddressURL else {
            statusMessage = "Neteisingas adresas."
            return
        }

        open(url)
    }

    private func open(_ url: URL) {
        address = BrowserURLBuilder.displayAddress(for: url)
        statusMessage = "Atidaroma: \(url.host(percentEncoded: false) ?? url.absoluteString)"
        addToHistory(url)
        openURL(url)
    }

    private func toggleFavoriteForCurrentAddress() {
        guard let url = normalizedAddressURL else { return }

        if favorites.contains(where: { $0.url == url }) {
            favorites.removeAll { $0.url == url }
            statusMessage = "Pasalinta is megstamu."
        } else {
            favorites.insert(BrowserFavorite(url: url), at: 0)
            statusMessage = "Issaugota megstamuose."
        }

        favorites = Array(favorites.prefix(8))
        persistFavorites()
    }

    private func addToHistory(_ url: URL) {
        let entry = BrowserHistoryEntry(url: url, visitedAt: Date())
        history.removeAll { $0.url == url }
        history.insert(entry, at: 0)
        history = Array(history.prefix(10))
        persistHistory()
    }

    private func clearHistory() {
        history.removeAll()
        persistHistory()
        statusMessage = "Istorija isvalyta."
    }

    private func persistIfNeeded() {
        if savedFavoritesData.isEmpty {
            persistFavorites()
        }
        if savedHistoryData.isEmpty {
            persistHistory()
        }
    }

    private func persistFavorites() {
        savedFavoritesData = BrowserStore.encodeFavorites(favorites)
    }

    private func persistHistory() {
        savedHistoryData = BrowserStore.encodeHistory(history)
    }
}

private struct BrowserRowButton: View {
    let title: String
    let subtitle: String
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color.white.opacity(0.15))
                    )

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.headline)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }

                Spacer(minLength: 0)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(8)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.white.opacity(0.06))
            )
        }
        .buttonStyle(.plain)
    }
}

struct BrowserFavorite: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let subtitle: String
    let icon: String
    let url: URL

    init(id: String? = nil, title: String, subtitle: String, icon: String, url: URL) {
        self.id = id ?? url.absoluteString
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.url = url
    }

    init(url: URL) {
        self.init(
            title: BrowserURLBuilder.title(for: url),
            subtitle: BrowserURLBuilder.displayAddress(for: url),
            icon: "star.fill",
            url: url
        )
    }

    var displayAddress: String {
        BrowserURLBuilder.displayAddress(for: url)
    }

    static let samples: [BrowserFavorite] = [
        BrowserFavorite(
            title: "Apple",
            subtitle: "apple.com",
            icon: "apple.logo",
            url: URL(string: "https://www.apple.com")!
        ),
        BrowserFavorite(
            title: "Google",
            subtitle: "google.com",
            icon: "magnifyingglass",
            url: URL(string: "https://www.google.com")!
        ),
        BrowserFavorite(
            title: "YouTube",
            subtitle: "youtube.com",
            icon: "play.rectangle.fill",
            url: URL(string: "https://www.youtube.com")!
        )
    ]
}

struct BrowserHistoryEntry: Identifiable, Codable, Equatable {
    let id: String
    let url: URL
    let visitedAt: Date

    init(url: URL, visitedAt: Date) {
        self.id = "\(url.absoluteString)-\(visitedAt.timeIntervalSince1970)"
        self.url = url
        self.visitedAt = visitedAt
    }

    var title: String {
        BrowserURLBuilder.title(for: url)
    }

    var subtitle: String {
        "\(BrowserURLBuilder.displayAddress(for: url)) • \(BrowserRelativeDateFormatter.string(from: visitedAt))"
    }
}

nonisolated enum BrowserStore {
    static func encodeFavorites(_ favorites: [BrowserFavorite]) -> String {
        encode(favorites)
    }

    static func decodeFavorites(from rawValue: String) -> [BrowserFavorite]? {
        decode([BrowserFavorite].self, from: rawValue)
    }

    static func encodeHistory(_ history: [BrowserHistoryEntry]) -> String {
        encode(history)
    }

    static func decodeHistory(from rawValue: String) -> [BrowserHistoryEntry]? {
        decode([BrowserHistoryEntry].self, from: rawValue)
    }

    private static func encode<T: Encodable>(_ value: T) -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]

        guard let data = try? encoder.encode(value),
              let string = String(data: data, encoding: .utf8) else {
            return ""
        }

        return string
    }

    private static func decode<T: Decodable>(_ type: T.Type, from rawValue: String) -> T? {
        guard !rawValue.isEmpty,
              let data = rawValue.data(using: .utf8) else {
            return nil
        }

        return try? JSONDecoder().decode(type, from: data)
    }
}

nonisolated enum BrowserURLBuilder {
    static func makeURL(from rawAddress: String) -> URL? {
        let trimmedAddress = rawAddress.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedAddress.isEmpty else { return nil }

        let addressWithScheme: String
        if trimmedAddress.contains("://") {
            addressWithScheme = trimmedAddress
        } else {
            addressWithScheme = "https://\(trimmedAddress)"
        }

        guard let components = URLComponents(string: addressWithScheme),
              let scheme = components.scheme?.lowercased(),
              ["http", "https"].contains(scheme),
              components.host?.isEmpty == false,
              let url = components.url else {
            return nil
        }

        return url
    }

    static func displayAddress(for url: URL) -> String {
        let host = url.host(percentEncoded: false) ?? url.absoluteString
        let path = url.path == "/" ? "" : url.path
        return host + path
    }

    static func title(for url: URL) -> String {
        let host = url.host(percentEncoded: false) ?? url.absoluteString
        let trimmedHost = host
            .replacingOccurrences(of: "www.", with: "")
            .components(separatedBy: ".")
            .first ?? host

        return trimmedHost.capitalized
    }
}

nonisolated enum BrowserRelativeDateFormatter {
    static func string(from date: Date, now: Date = Date()) -> String {
        let seconds = Int(now.timeIntervalSince(date))
        if seconds < 60 { return "ka tik" }

        let minutes = seconds / 60
        if minutes < 60 { return "\(minutes) min. pries" }

        let hours = minutes / 60
        if hours < 24 { return "\(hours) val. pries" }

        let days = hours / 24
        return "\(days) d. pries"
    }
}

#Preview {
    NavigationStack {
        ContentView()
    }
}
