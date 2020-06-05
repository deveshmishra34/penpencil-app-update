import Foundation
import Capacitor
import Zip

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(AppUpdatePlugin)
public class AppUpdatePlugin: CAPPlugin {

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.success([
            "value": value
        ])
    }

    @objc func getAppInfo(_ call: CAPPluginCall) {
        let documentsUrl =  FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask).first!
        let pref = UserDefaults.standard

        let buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String
        let buildNumberName = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
        let packageName = Bundle.main.infoDictionary?["CFBundleIdentifier"] as? String

        let udid = UIDevice.current.identifierForVendor?.uuidString
        let name = UIDevice.current.name
        let version = UIDevice.current.systemVersion
        let modelName = UIDevice.current.model
        let updateVersion = pref.string(forKey: "updateVersion") ?? ""
        let updateStatus = pref.string(forKey: "updateStatus")
        let updateUrl = documentsUrl.appendingPathComponent("NoCloud/penpencil-updates/" + updateVersion + "/www")//pref.string(forKey: "updateUrl")

        let successData:[String: Any] = [
                "version": buildNumber ?? "",
                "binaryVersionCode": buildNumber ?? "",
                "bundleName": packageName ?? "",
                "bundleVersion": buildNumberName ?? "",
                "binaryVersionName": buildNumberName ?? "",
                "deviceInfo": [
                    "platform": "ios",
                    "manufacturer": "Apple",
                    "brand": "Apple",
                    "model": name,
                    "platformVersion": version,
                    "hardware": modelName,
                    "display": modelName
                ],
                "deviceId": udid ?? "",
                "updateVersion": updateVersion,
                "updateStatus": updateStatus ?? "",
                "updateUrl": updateUrl.absoluteString
        ]

        call.resolve(successData);
    }

    @objc func updatePref(_ call: CAPPluginCall) {
        let data: [String] = [
            call.getString("updateVersion") ?? "",
            call.getString("updateStatus") ?? "",
            call.getString("updateUrl") ?? "",
        ]

        updateUserPref(data: data)
        call.resolve();
    }

    @objc func downloadUpdate(_ call: CAPPluginCall) {
        let fileUrl = call.getString("fileUrl") ?? ""; // downloadable URL
        let fileName = call.getString("fileName") ?? ""; // fileName.zip
        let url = URL(string: fileUrl)!;

        let documentsUrl =  FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask).first!
        let destinationUrl = documentsUrl.appendingPathComponent("NoCloud/" + fileName)

        if FileManager().fileExists(atPath: destinationUrl.path) {
            self.notifyListeners("appUpdateDownloaded", data: [:])
            print("AppUpdateService","File already exists [\(destinationUrl.path)]")
            return
        }

        loadFileAsync(url: url) { (path, error) in
            print("AppUpdateService", "Update downloaded to : \(path!)")
            self.notifyListeners("appUpdateDownloaded", data: [:])

            let data: [String] = [
                "",
                "Ready",
                 ""
            ]
            self.updateUserPref(data: data);
        }

        call.resolve();

    }

    @objc func copyAndExtractFile(_ call: CAPPluginCall) {
        let fileName = call.getString("fileName") ?? ""; // fileName.zip
        let updateVersion = call.getString("updateVersion") ?? ""; // updateVersion version

        print("AppUpdateService", "copyAndExtractFile")

        let documentsUrl =  FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask).first!
        let sourcesUrl = documentsUrl.appendingPathComponent("NoCloud/" + fileName)
        let destinationUrl = documentsUrl.appendingPathComponent("NoCloud/penpencil-updates/" + updateVersion)
        let updateUrl = "NoCloud/penpencil-updates/" + updateVersion + "/www"

        let deleteSource = sourcesUrl.absoluteURL

        print("AppUpdateService", "sourcesUrl", sourcesUrl);
        print("AppUpdateService", "destinationUrl", destinationUrl);
        print("AppUpdateService", "updateUrl", updateUrl);

        do {
            try Zip.unzipFile(sourcesUrl, destination: destinationUrl, overwrite: true, password: "")
            self.notifyListeners("appUpdateLive", data: [:])
            let data: [String] = [
                "",
                "Live",
                updateUrl
            ]
            self.updateUserPref(data: data);
            try FileManager.default.removeItem(at: deleteSource);
            removExtraUpdates();
            call.resolve();
        } catch {
            print("AppUpdateService",  "Extraction of ZIP archive failed with error:\(error)")
            let data: [String] = [
                "",
                "Downloading",
                 ""
            ]
            self.updateUserPref(data: data);
        }
    }

//    func loadFileSync(url: URL, completion: @escaping (String?, Error?) -> Void) {
//        let documentsUrl = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
//
//        let destinationUrl = documentsUrl.appendingPathComponent(url.lastPathComponent)
//
//        if FileManager().fileExists(atPath: destinationUrl.path)
//        {
//            print("AppUpdateService","File already exists [\(destinationUrl.path)]")
//            completion(destinationUrl.path, nil)
//        }
//        else if let dataFromURL = NSData(contentsOf: url)
//        {
//            if dataFromURL.write(to: destinationUrl, atomically: true)
//            {
//                print("AppUpdateService","file saved [\(destinationUrl.path)]")
//                completion(destinationUrl.path, nil)
//            }
//            else
//            {
//                print("AppUpdateService","error saving file")
//                let error = NSError(domain:"Error saving file", code:1001, userInfo:nil)
//                completion(destinationUrl.path, error)
//            }
//        }
//        else
//        {
//            let error = NSError(domain:"Error downloading file", code:1002, userInfo:nil)
//            completion(destinationUrl.path, error)
//        }
//    }

    func loadFileAsync(url: URL, completion: @escaping (String?, Error?) -> Void) {
        let documentsUrl =  FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask).first!
        let destinationUrl = documentsUrl.appendingPathComponent("NoCloud/" + url.lastPathComponent)

        if FileManager().fileExists(atPath: destinationUrl.path) {
            print("AppUpdateService","File already exists [\(destinationUrl.path)]")
            completion(destinationUrl.path, nil)
        } else {
            let session = URLSession(configuration: URLSessionConfiguration.default, delegate: nil, delegateQueue: nil)
            var request = URLRequest(url: url)
            request.httpMethod = "GET"
            let task = session.dataTask(with: request, completionHandler: {
                data, response, error in
                if error == nil {
                    if let response = response as? HTTPURLResponse {
                        if response.statusCode == 200 {
                            if let data = data {
                                if let _ = try? data.write(to: destinationUrl, options: Data.WritingOptions.atomic) {
                                    completion(destinationUrl.path, error)
                                } else {
                                    completion(destinationUrl.path, error)
                                }
                            } else {
                                completion(destinationUrl.path, error)
                            }
                        }
                    }
                } else {
                    completion(destinationUrl.path, error)
                }
            })
            task.resume()
        }
    }

    func updateUserPref(data: [String]) {
        let updateVersion = data[0]
        let updateStatus = data[1]
        let updateUrl = data[2]

        let pref = UserDefaults.standard

        if (!updateVersion.isEmpty) {
            pref.set(updateVersion, forKey: "updateVersion")
        }

        if (!updateStatus.isEmpty) {
            pref.set(updateStatus, forKey: "updateStatus");
        }

        if (!updateUrl.isEmpty) {
            pref.set(updateUrl, forKey: "updateUrl");
        }
    }

    func removExtraUpdates() {
        let pref = UserDefaults.standard
        let currentUpdateVersion = pref.string(forKey: "updateVersion")!
        let fileManager = FileManager.default
        let documentsURL = fileManager.urls(for: .libraryDirectory, in: .userDomainMask).first!
        let penpeencilUpdateDir = documentsURL.appendingPathComponent("NoCloud/penpencil-updates/")
        do {
            let fileURLs = try fileManager.contentsOfDirectory(at: penpeencilUpdateDir, includingPropertiesForKeys: nil)
            let count = fileURLs.count

            for i in 0..<count
            {
                if (fileManager.fileExists(atPath: fileURLs[i].absoluteString) != true && !fileURLs[i].absoluteString.contains(currentUpdateVersion))
                {
                    print("File is \(fileURLs[i])");
                    try FileManager.default.removeItem(at: fileURLs[i])
                }
            }
            // process files
        } catch {
            print("Error while enumerating files \(documentsURL.path): \(error.localizedDescription)")
        }
    }

}
