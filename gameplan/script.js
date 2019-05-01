$('#sign-in').on('click', function() {
	if ($('#initials-start').val().length == 2) {
		signIn();
	}
});

$('#initials-start').keypress(function(e) {
	if (e.which == 13 && $(this).val().length == 2) {
		signIn();
	}
});

$('#sign-out').on('click', function() {
	if ($('#initials-end').val().length == 2) {
		signOut();
	}
});

$('#initials-end').keypress(function(e) {
	if (e.which == 13 && $(this).val().length == 2) {
		signOut();
	}
});

$('.plan input').each(function() {
	$(this).on('click', setBtn);
});

function signIn() {
	// TODO: enable table when signed in
	console.log(moment().format());
	$('.cell__sign-in').html('Signed in by ' + $('#initials-start').val() + '<br> at ' + moment().format('hh:mm a'));
	$('.cell__sign-out').css('opacity','1');
}

function signOut() {
	// TODO: disable table when signed out
	console.log(moment().format());
	$('.cell__sign-out').html('Signed out by ' + $('#initials-end').val() + '<br> at ' + moment().format('hh:mm a'));
}

function setRow(i) {
}

function setBtn() {
	if ($(this).hasClass('complete')) {
		$(this).removeClass('complete');
		$(this).addClass('todo');
	}
	else if ($(this).hasClass('incomplete')) {
		$(this).removeClass('incomplete');
		$(this).addClass('complete');
	}
	else {
		$(this).addClass('incomplete');
		$(this).removeClass('todo');
	}

}