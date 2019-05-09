<html>
	<head>
		<title>Arup Mathnasium</title>
        <link rel="stylesheet" type="text/css" href="style.css">
	</head>
	<body>
		<div class="message"></div>
		<div class="login">
			<label>Student ID:</label>
				<form action="attendance.php" method="post">
				<input type="text" name="studentID">
				<input type="submit">
			</form>
		</div>
	</body>
	<script>
		var timeout;
		console.log(<?php echo json_encode($_POST) ?>);

		function message(studentID) {
			const message = document.querySelector('.message');
			message.textContent = 'Welcome, ' + studentID + '!';
			if (timeout) {
				clearTimeout(timeout);
			}
			timeout = setTimeout(function() {
				message.textContent = '';
			},2750);
		}
	</script>
	<?php
		if(isset($_COOKIE["radius_cookies"]) && isset($_COOKIE["radius_token"])) {
			if (!empty($_POST["studentID"])) {
				$studentID = htmlentities($_POST["studentID"]);

				if (strlen(trim($studentID)) > 0) {
					// send request to Radius and MB
					
					// if successful, use js to display
					echo "<script>message(\"{$studentID}\");</script>";
				}
			}
		}
		else {
			echo "Not logged in. >:(";
		}
	?>	
</html>