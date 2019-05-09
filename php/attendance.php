<html>
	<head>
		<title>Arup Mathnasium</title>
        <link rel="stylesheet" type="text/css" href="style.css">
	</head>
	<body>
		<div class="message"></div>
		<div class="lastActivity"></div>
		<div class="login">
			<label>Student ID:</label>
				<form action="attendance.php" method="post">
				<input type="text" name="studentID">
				<input type="submit">
				<!-- <select name="location">
    				<option disabled selected>Select location</option>
					<option value="30">Ellicott City</option>
					<option value="32">Germantown</option>
					<option value="29">North Potomac</option>
					<option value="3050">Potomac</option>
					<option value="31">Rockville</option>
				</select> -->
			</form>
		</div>
	</body>
	<script>
		var timeout;
		console.log(<?php echo json_encode($_POST) ?>);

		function message(studentID, lastActivity, isCheckedIn) {
			const message = document.querySelector('.message');
			const lastActivityDiv = document.querySelector('.lastActivity');
			if (isCheckedIn) {
				message.textContent = 'Welcome, ' + studentID + '!';
			}
			else {
				message.textContent = 'Goodbye, ' + studentID + '!';
			}
			lastActivityDiv.textContent = lastActivity;

			if (timeout) {
				clearTimeout(timeout);
			}
			timeout = setTimeout(function() {
				message.textContent = '';
				lastActivityDiv.textContent = '';
			},2750);
		}

		function select(location) {
			const select = document.querySelectorAll('option');
			for (var i = 0; i < select.length; i++) {
				if (select[i].getAttribute('value') == location) {
					select[i].selected = 'selected';
					break;
				}
			}
		}
	</script>
	<?php
		if(isset($_COOKIE["radius_cookies"]) && isset($_COOKIE["radius_token"])) {
			// if (!empty($_POST["location"])) {
				// $location = $_POST["location"];
				// echo "<script>select({$location});</script>";
				if (!empty($_POST["studentID"])) {
					$studentID = htmlentities($_POST["studentID"]);
					if (strlen(trim($studentID)) > 0 ) {
						// send request to Radius and MB
						$ch = curl_init('https://radius.mathnasium.com/Attendance/StudentAttendances_Read?centerId=');
						curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
						curl_setopt($ch, CURLOPT_FOLLOWLOCATION, false);
						curl_setopt($ch, CURLOPT_HTTPHEADER, array("Cookie: {$_COOKIE["radius_cookies"]}"));
						$result = curl_exec($ch);
						$cookies = "";
						preg_match_all('/^Set-Cookie:\s*([^;]*)/mi', $result, $matches);
						foreach($matches[1] as $item) {
							$cookies = "{$cookies}{$item}; ";
						}
						$httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
						if ($httpcode == 200 && !empty($result)) {
							$json = json_decode($result, true);
							if (is_numeric($studentID)) {
								foreach($json["Data"] as $item) {
									if ($item["StudentID"] == $studentID) {
										// if successful, use js to display
										echo "<script>message(\"{$item["StudentName"]}\");</script>";
										break;
									}
								}
							}
							else {
								foreach($json["Data"] as $item) {
									if (strtoupper($item["StudentName"]) == strtoupper($studentID)) {
										$ch = curl_init('http://radius.mathnasium.com/Attendance/StudentCheckIn/30');
										curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
										curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true); // this one redirects!
										curl_setopt($ch, CURLOPT_HTTPHEADER, array(
											"Cookie: {$_COOKIE["radius_cookies"]}",
											"Referer: https://radius.mathnasium.com/",
											"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
										));
										$result = curl_exec($ch);
										$doc = new DomDocument();
										libxml_use_internal_errors(true);
										$doc->loadHTML($result);
										libxml_clear_errors();
										$xpath = new DomXpath($doc);
										$entries = $xpath->query("//input[@name='__RequestVerificationToken']/@value");
										if($entries->length > 0) {
											$node = $entries->item(0);
											$signInToken = $node->nodeValue;
										}

										// check if currently signed in (TODO)
										if ($item["IsCheckedIn"])
											$attendanceID = $item["AttendanceID"];
										else $attendanceID = 0;

										$data = array(
											"StudentID" => $item["StudentID"],
											"AttendanceID" => $attendanceID,
											"EnrollmentId" => $item["EnrollmentID"],
											"IsRoster" => false,
											"RosterUID" => $item["RosterUID"]
										);
										$data_string = json_encode($data);
										$ch = curl_init('https://radius.mathnasium.com/Attendance/ChangeCheckInStatus');

										curl_setopt($ch, CURLOPT_POSTFIELDS, $data_string);
										curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
										curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true); // this one redirects!
										curl_setopt($ch, CURLOPT_POST, 1);
										curl_setopt($ch, CURLOPT_HTTPHEADER, array(
										    "Content-Type: application/json; charset=UTF-8",
										    "__requestverificationtoken: {$signInToken}",
										    "Origin: https://radius.mathnasium.com/",
										    "Referer: https://radius.mathnasium.com/Attendance/StudentCheckIn/30",
										    "Cookie: {$_COOKIE["radius_cookies"]}",
										    "x-requested-with: XMLHttpRequest"
										));
										$result = curl_exec($ch);
										$httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
										if ($httpcode == 200) {
											// if successful, use js to display
											echo "<script>message(\"{$item["StudentName"]}\",\"{$item["LastActivity"]}\",{$item["IsCheckedIn"]});</script>";
										}
										break;
									}
								}
							}
						}
					}
				}
			// }
			// else {
			// 	echo "No location selected!";
			// }
		}
		else {
			echo "Not logged in. >:(";
		}
	?>	
</html>