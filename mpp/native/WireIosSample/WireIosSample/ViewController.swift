import UIKit
import Wire

class ViewController: UIViewController {
	@IBOutlet var encodedBytesView: UITextView!
	
	@IBOutlet var encodedNameView: UITextField!
	@IBOutlet var encodedPeriodView: UITextField!
	@IBOutlet var encodedLengthView: UITextField!
	@IBOutlet var encodedMassView: UITextField!
	
	@IBOutlet var decodedNameView: UITextField!
	@IBOutlet var decodedPeriodView: UITextField!
	@IBOutlet var decodedLengthView: UITextField!
	@IBOutlet var decodedMassView: UITextField!
	
	override func viewDidLoad() {
		super.viewDidLoad()
		
		// Create an immutable value object with the Builder API.
		let stegosaurus = Dinosaur(
			name: "Stegosaurus" as String?,
			picture_urls: ["http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"],
			length_meters: 9.0 as KotlinDouble?,
			mass_kilograms: 5_000.0 as KotlinDouble?,
			period: .jurassic,
			unknownFields: OkioKt.decodeBase64(base64: "")
		)
		
		let dinosaurAdapter = Dinosaur.Companion().ADAPTER

		// Encode that value to bytes, and print that as base64.
		let stegosaurusEncoded = dinosaurAdapter.encode(value: stegosaurus)
		encodedBytesView.text = OkioKt.encodeBase64(data: stegosaurusEncoded)
		
		let stegosaurusEncodedAgain = OkioKt.decodeBase64(base64: encodedBytesView.text)
		let stegosaurusAgain = dinosaurAdapter.decode(bytes_: stegosaurusEncodedAgain) as! Dinosaur
		encodedNameView.text = stegosaurusAgain.name
		encodedPeriodView.text = stegosaurusAgain.period!.name
		encodedLengthView.text = "\(stegosaurusAgain.length_meters!) m"
		encodedMassView.text = "\(stegosaurusAgain.mass_kilograms!) kg"
		
		// Decode base64 bytes, and decode those bytes as a dinosaur.
		let tyrannosaurusEncoded = OkioKt.decodeBase64(base64: "Cg1UeXJhbm5vc2F1cnVzEmhodHRwOi8vdmln"
        + "bmV0dGUxLndpa2lhLm5vY29va2llLm5ldC9qdXJhc3NpY3BhcmsvaW1hZ2VzLzYvNmEvTGVnbzUuanBnL3Jldmlz"
        + "aW9uL2xhdGVzdD9jYj0yMDE1MDMxOTAxMTIyMRJtaHR0cDovL3ZpZ25ldHRlMy53aWtpYS5ub2Nvb2tpZS5uZXQv"
        + "anVyYXNzaWNwYXJrL2ltYWdlcy81LzUwL1JleHlfcHJlcGFyaW5nX2Zvcl9iYXR0bGVfd2l0aF9JbmRvbWludXNf"
        + "cmV4LmpwZxmamZmZmZkoQCEAAAAAAJC6QCgB")
		let tyrannosaurus = dinosaurAdapter.decode(bytes_: tyrannosaurusEncoded) as! Dinosaur
		
		decodedNameView.text = tyrannosaurus.name
		decodedPeriodView.text = tyrannosaurus.period!.name
		decodedLengthView.text = "\(tyrannosaurus.length_meters!) m"
		decodedMassView.text = "\(tyrannosaurus.mass_kilograms!) kg"

		setupScrollingImages(tyrannosaurus.picture_urls)
	}
	
	fileprivate func setupScrollingImages(_ pictureUrls: [String]) {
		let screensize: CGRect = UIScreen.main.bounds
		let screenWidth = screensize.width
		let screenHeight = screensize.height
		let massViewRight = decodedMassView.globalFrame.maxX
		let massViewBottom = decodedMassView.globalFrame.maxY
		let inset = screenWidth - massViewRight
		
		let scrollView = UIScrollView(
			frame: CGRect(x: inset,
						  y: massViewBottom + inset,
						  width: screenWidth - inset * 2,
						  height: (screenHeight - (inset + massViewBottom))
			)
		)
		scrollView.contentSize = CGSize(
			width: screenWidth - inset * 2,
			height: (screenHeight - (inset + massViewBottom)) * 3
		)
		view.addSubview(scrollView)
		
		var nextY: CGFloat = 0.0
		for pictureUrl in pictureUrls {
			let imageUrl = URL(string: pictureUrl)
			let imageData = try? Data(contentsOf: imageUrl!)
			let imageView = UIImageView(
				frame: CGRect(x: 0,
							  y: nextY,
							  width: screenWidth - inset * 2,
							  height: screenWidth - inset * 2
				)
			)
			imageView.image = UIImage(data: imageData!)
			imageView.contentMode = .scaleAspectFit
			
			scrollView.addSubview(imageView)
			
			nextY = nextY + imageView.bounds.height
		}
	}
}

extension UIView {
    var globalFrame: CGRect {
        let rootView = UIApplication.shared.keyWindow?.rootViewController?.view
		return self.superview?.convert(self.frame, to: rootView) ?? CGRect.zero
    }
}

