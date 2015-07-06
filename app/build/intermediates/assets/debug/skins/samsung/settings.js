var nameHelp = "Enter your first and last name";
var pinHelp  = "Enter a four digit pincode that protects your private data on this app."
pinHelp += ' The code must consiste of 4 numbers';
var phoneHelp='Enter the phonenumber where you can be reached';
phoneHelp +=' The number MUST include your countrycode (with either + or 00 format)'
var inputErrorHelp = 'Your data cannot be saved due to incorrect input(s)';
var pinErrorHelp ='Your pincode is not correct. it MUST consist of 4 numbers';
var phoneErrorHelp = 'Incorrect telephone format. Your phone number must be preceded by your country code.';
phoneErrorHelp += '(for example 0039 or +39 for Italy countrycode).';
phoneErrorHelp += ' The countrycode must be directly followed by your phone number without any dashes (-) spaces or other non numeric characters';
var nameErrorHelp = 'Please fill in your name';

var amountHelp ='Please fill in the desired amount. The currency is Euro';
var creditcardHelp ="Please fill in the numbers of your creditcard without spaces or any other alfanumeric characters";
var expirationHelp = 'Expritation date on card in MM and YYYY format';
var securityCodeHelp = 'The security code is a 3 digit nnumber that you can find on the back of your card';
var nameOnCardHelp ='Type in your name as it appears on the card';


var pinError = false;
var phoneError = false;
var nameError = false;

function EM_proxy(action,arguments) {
  var payload = {
    action: action,
    arguments: arguments
  }
  url = JSON.stringify(payload);
  //console.log(url);
  location.replace(url);
  
}


function save_user (){
  var user = {
		  
    name: jQuery('#page_5 #name').val(),
    code: jQuery('#page_5 #code').val(),
    phone: jQuery('#page_5 #phone').val(),
    
  };
  nameCheck();
  phoneCheck();
  pinCheck();
  if ( (!nameError) && (!pinError) && (!phoneError)) EM_proxy('saveUser',user);
  else dialogOpen(inputErrorHelp);
}

function update_wallet(){
	var transaction = {
		    amount : jQuery('#amount').val(),
		    ccType : 'master',
		    ccNumber :  jQuery('#ccNumber').val(),
		    expirationMonth: jQuery('#expirationMonth').val(),
		    expirationYear: jQuery('#expirationYear').val(),
		    securityCode: jQuery('#securityCode').val(),
		    nameOnCard: jQuery('#nameOnCard').val(),
		    
		  };
	EM_proxy('updateWallet',transaction);	  
	
}


function switch_to_page (id) {
  jQuery('.page').hide();
  jQuery('#page_' + id).show(); 
}

function dialogOpen(text){
	jQuery('#dialog .content').html(text);
	jQuery('#dialog').show();
}

function dialogClose(){
	jQuery('#dialog').hide();
}

function pinCheck(){
	var pin = jQuery('#code').val();
	if (pin.length !=4) {
		pinError= true;
		dialogOpen(pinErrorHelp);
		return;
	}
	if (isNaN(pin) ) {
		dialogOpen(pinErrorHelp);
		pinError= true;
		return;
	}
	else pinError = false;
	 
}

function nameCheck(){
	var name = jQuery('#name').val();
	if (name.length == 0 ) {
		nameError= true;
		dialogOpen(nameErrorHelp);
		return;
	}
	else nameError = false;
	 
}

function phoneCheck () {
	var phone = jQuery('#phone').val();
	if (phone.charAt(0) =='+') {
		if (!isNaN(phone)) {
			phoneError=false;
		    return;
			
		}
	}
	if ((phone.charAt(0) =='0')&&(phone.charAt(1) =='0')) {
		if (!isNaN(phone)) {
			phoneError=false;
		    return;
			
		}
	}
	phoneError = true;
	dialogOpen(phoneErrorHelp);
}



function creditCardCheck() {
	
}
function expirationMonthCheck() {
	
}
function expirationYaerCheck() {
	
}
function securityCodeCheck(){
	
}

jQuery('document').ready(function (){
 // debug   
    dialogClose();
    switch_to_page(5);
  
  // end debug
      
  });
     
  
 