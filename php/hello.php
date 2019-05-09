<html>
	<head>
		<title>Arup Mathnasium</title>
        <link rel="stylesheet" type="text/css" href="style.css">
	</head>
</html>
<?php 
	$ch = curl_init('https://radius.mathnasium.com/Account/Login');
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	// get headers too with this line
	curl_setopt($ch, CURLOPT_HEADER, 1);
	$result = curl_exec($ch);
	$httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
	// echo "HTTP Code: {$httpcode}"; // 200
	// get cookies
	preg_match_all('/^Set-Cookie:\s*([^;]*)/mi', $result, $matches);
	// $cookies = array();
	$cookies = "";
	foreach($matches[1] as $item) {
		$cookies = "{$cookies}{$item}; ";
	    // parse_str($item, $cookie);
	    // $cookies = array_merge($cookies, $cookie);
	}
	// echo "<br>";
	// echo "Cookies: {$cookies}";
	// echo "<br>";
	// get site verification token
	$doc = new DomDocument();
	libxml_use_internal_errors(true);
	$doc->loadHTML($result);
	libxml_clear_errors();
	$xpath = new DomXpath($doc);
	$entries = $xpath->query("//input[@name='__RequestVerificationToken']/@value");
	if($entries->length > 0) {
		$node = $entries->item(0);
		$token = $node->nodeValue;
	}
	// echo "Request Verification Token: ";
	// echo $token;
	// echo "<br>";
	curl_close($ch);

	$ch = curl_init("https://radius.mathnasium.com/Account/Login");
	if(isset($_COOKIE["radius_cookies"]) && isset($_COOKIE["radius_token"])) {
		// cookie already exist
		include("attendance.php");
	}
	else {
		if (!empty($_POST["username"]) && !empty(["password"])) {
			// resends form request
			$username = $_POST["username"];
			$password = $_POST["password"];
			curl_setopt($ch, CURLOPT_POST, 1);
			curl_setopt($ch, CURLOPT_POSTFIELDS,
			            "UserName={$username}&Password={$password}&RememberMe=false&__RequestVerificationToken={$token}");
			curl_setopt($ch, CURLOPT_HTTPHEADER, array(
				'Content-Type: application/x-www-form-urlencoded',
				'Origin: https://radius.mathnasium.com',
				'Referer: https://radius.mathnasium.com/Account/Login',
				'User-agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36',
				'Upgrade-insecure-requests: 1',
				'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3',
				'Accept-encoding: gzip, deflate, br',
				'Accept-language: en-US,en;q=0.9',
				"Cookie: {$cookies}"
			));

			// receive server response
			curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
			curl_setopt($ch, CURLOPT_HEADER, 1);
			curl_setopt($ch, CURLOPT_FOLLOWLOCATION, false); //don't follow redirects
			$result = curl_exec ($ch);
			// $header_size = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
			// $header = substr($result, 0, $header_size);
			// echo "<br>{$header}<br><br>"; // print entire header
			$httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
			// echo "HTTP Code: {$httpcode}"; // 302
			$cookies = "";
			preg_match_all('/^Set-Cookie:\s*([^;]*)/mi', $result, $matches);
			foreach($matches[1] as $item) {
				$cookies = "{$cookies}{$item}; ";
			}
			// echo "<br>";
			// echo "New Cookies: {$cookies}";
			// echo "<br>";
			if ($httpcode == 302) {
				// get new token
				$ch = curl_init('https://radius.mathnasium.com');
				curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
				curl_setopt($ch, CURLOPT_HTTPHEADER, array("Cookie: {$cookies}"));
				$result = curl_exec($ch);
				$doc = new DomDocument();
				libxml_use_internal_errors(true);
				$doc->loadHTML($result);
				libxml_clear_errors();
				$xpath = new DomXpath($doc);
				$entries = $xpath->query("//input[@name='__RequestVerificationToken']/@value");
				if($entries->length > 0) {
					$node = $entries->item(0);
					$token = $node->nodeValue;
				}
				setcookie("radius_cookies", $cookies, time() + (86400 * 30), "/"); // 86400 = 1 day
				setcookie("radius_token", $token, time() + (86400 * 30), "/"); // 86400 = 1 day
    			$_COOKIE["radius_cookies"] = $cookies; // workaround
    			$_COOKIE["radius_token"] = $token;
				include("attendance.php");
				echo "Logged in.";
			}
			else {
				echo "Login failed.";
			}

			curl_close($ch);
		}
		else {
			echo "Not logged in.";
		}
	}
?>