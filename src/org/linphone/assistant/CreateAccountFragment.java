package org.linphone.assistant;
/*
CreateAccountFragment.java
Copyright (C) 2015  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.LinphoneXmlRpcRequest;
import org.linphone.core.LinphoneXmlRpcRequest.LinphoneXmlRpcRequestListener;
import org.linphone.core.LinphoneXmlRpcRequestImpl;
import org.linphone.core.LinphoneXmlRpcSession;
import org.linphone.core.LinphoneXmlRpcSessionImpl;
import org.linphone.xmlrpc.XmlRpcHelper;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
/**
 * @author Sylvain Berfini
 */
public class CreateAccountFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, OnClickListener {
	private Handler mHandler = new Handler();
	private EditText phoneNumberEdit, usernameEdit, passwordEdit, passwordConfirmEdit, emailEdit, dialCode;
	private TextView phoneNumberError, usernameError, passwordError, passwordConfirmError, emailError, sipUri;
	
	private boolean phoneNumberOk = false;
	private boolean usernameOk = false;
	private boolean passwordOk = false;
	private boolean emailOk = false;
	private boolean confirmPasswordOk = false;
	private Button createAccount, selectCountry;
	private CheckBox useUsername, useEmail;
	private LinearLayout phoneNumberLayout, usernameLayout, emailLayout;
	private final Pattern UPPER_CASE_REGEX = Pattern.compile("[A-Z]");
	private LinphoneXmlRpcSession xmlRpcSession;
	private CountryListFragment.Country country;
	
	private String getUsername() {
		String username = usernameEdit.getText().toString();
		return username.toLowerCase(Locale.getDefault());
	}

	private String getPhoneNumber(){
		LinphoneProxyConfig proxyConfig = LinphoneManager.getLc().createProxyConfig();
		String countryCode = dialCode.getText().toString();
		if(countryCode != null && countryCode.startsWith("+")) {
			countryCode = countryCode.substring(1);
		}
		proxyConfig.setDialPrefix(countryCode);
		return proxyConfig.normalizePhoneNumber(phoneNumberEdit.getText().toString());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.assistant_account_creation, container, false);

		phoneNumberError = (TextView) view.findViewById(R.id.phone_number_error);
		phoneNumberEdit = (EditText) view.findViewById(R.id.phone_number);
		phoneNumberEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {}
		});
		phoneNumberLayout = (LinearLayout) view.findViewById(R.id.phone_number_layout);
		addXMLRPCPhoneNumberHandler(phoneNumberEdit, null);

		selectCountry = (Button) view.findViewById(R.id.select_country);
		selectCountry.setOnClickListener(this);

		dialCode = (EditText) view.findViewById(R.id.dial_code);
		sipUri = (TextView) view.findViewById(R.id.sip_uri);


		if(getResources().getBoolean(R.bool.assistant_allow_username)) {
			useUsername = (CheckBox) view.findViewById(R.id.use_username);
			useUsername.setVisibility(View.VISIBLE);
			useUsername.setOnCheckedChangeListener(this);

			usernameError = (TextView) view.findViewById(R.id.username_error);
			usernameEdit = (EditText) view.findViewById(R.id.username);
			usernameEdit.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					/*if(useUsername.isChecked() && usernameEdit.getText().length() > 0) {
						sipUri.setText("Sip uri is sip:" + s.toString() + "@sip.linphone.org");
					} else {
						if(phoneNumberEdit.getText().length() > 0){
							sipUri.setText("Sip uri is sip:" + phoneNumberEdit.getText().toString()+ "@sip.linphone.org");
						} else {
							sipUri.setText("");
						}
					}*/
				}

				@Override
				public void afterTextChanged(Editable s) {}
			});
			usernameLayout = (LinearLayout) view.findViewById(R.id.username_layout);
			addXMLRPCUsernameHandler(usernameEdit, null);
		}

		if(getResources().getBoolean(R.bool.isTablet)){
			useEmail.setVisibility(View.VISIBLE);
			useEmail = (CheckBox) view.findViewById(R.id.use_email);
			useEmail.setOnCheckedChangeListener(this);

			passwordError = (TextView) view.findViewById(R.id.password_error);
			passwordEdit = (EditText) view.findViewById(R.id.password);

			passwordConfirmError = (TextView) view.findViewById(R.id.confirm_password_error);
			passwordConfirmEdit = (EditText) view.findViewById(R.id.confirm_password);

			emailError = (TextView) view.findViewById(R.id.email_error);
			emailEdit = (EditText) view.findViewById(R.id.email);
			emailLayout = (LinearLayout) view.findViewById(R.id.email_layout);

			addXMLRPCPasswordHandler(passwordEdit, null);
			addXMLRPCConfirmPasswordHandler(passwordEdit, passwordConfirmEdit, null);
			addXMLRPCEmailHandler(emailEdit, null);

			if (getResources().getBoolean(R.bool.pre_fill_email_in_wizard)) {
				Account[] accounts = AccountManager.get(getActivity()).getAccountsByType("com.google");

				for (Account account: accounts) {
					if (isEmailCorrect(account.name)) {
						String possibleEmail = account.name;
						emailEdit.setText(possibleEmail);
						emailOk = true;
						break;
					}
				}
			}
		}

		String previousPhone = AssistantActivity.instance().phone_number;
		if(previousPhone != null ){
			phoneNumberEdit.setText(previousPhone);
		}
		setCountry(AssistantActivity.instance().country);
		updateApplyButton();

    	createAccount = (Button) view.findViewById(R.id.assistant_create);
    	createAccount.setEnabled(true);
    	createAccount.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(getResources().getBoolean(R.bool.isTablet)) {
					if (useEmail.isChecked()) {
						createAccountWithEmail(getUsername(), passwordEdit.getText().toString(), emailEdit.getText().toString(), false);
					}
				} else {
					createAccountWithPhoneNumber(getUsername(), "", getPhoneNumber(), false);
				}
			}
		});
    	


		int phoneStatePermission = getActivity().getPackageManager().checkPermission(Manifest.permission.RECORD_AUDIO, getActivity().getPackageName());

		if (phoneStatePermission == PackageManager.PERMISSION_GRANTED) {
			TelephonyManager mTelephonyManager;
			mTelephonyManager = (TelephonyManager) getActivity().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			String yourNumber = mTelephonyManager.getLine1Number();
			String iso = mTelephonyManager.getNetworkCountryIso();
		}

		xmlRpcSession = new LinphoneXmlRpcSessionImpl(LinphoneManager.getLcIfManagerNotDestroyedOrNull(), getString(R.string.wizard_url));
    	
		return view;
	}

	private void updateApplyButton() {

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(buttonView.getId() == R.id.use_username) {
			if(isChecked) {
				usernameLayout.setVisibility(View.VISIBLE);
				if(usernameEdit.getText().length() > 0){
					sipUri.setText("Sip uri is sip:" + usernameEdit.getText().toString() + "@sip.linphone.org");
				}
			} else {
				usernameLayout.setVisibility(View.GONE);
				if(phoneNumberEdit.getText().length() > 0){
					sipUri.setText("Sip uri is sip:" + phoneNumberEdit.getText().toString() + "@sip.linphone.org");
				}
			}
		} else if(buttonView.getId() == R.id.use_email){
			if(isChecked) {
				emailLayout.setVisibility(View.VISIBLE);
				usernameLayout.setVisibility(View.VISIBLE);
				useUsername.setEnabled(false);
			} else {
				if(!useUsername.isChecked()) {
					usernameLayout.setVisibility(View.GONE);
				}
				emailLayout.setVisibility(View.GONE);
				usernameLayout.setVisibility(View.GONE);
				useUsername.setEnabled(true);
			}
		}
	}

	public void setCountry(CountryListFragment.Country c) {
		country = c;
		if( c!= null) {
			dialCode.setText(c.dial_code);
			selectCountry.setText(c.name);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.select_country: {
				AssistantActivity.instance().displayCountryChooser();
			}
		}
	}

	private void displayError(Boolean isOk, TextView error, EditText editText, String errorText){
		if(isOk || editText.getText().toString().equals("")){
			error.setVisibility(View.INVISIBLE);
			error.setText(errorText);
			editText.setBackgroundResource(R.drawable.resizable_textfield);
		} else {
			error.setVisibility(View.VISIBLE);
			error.setText(errorText);
			editText.setBackgroundResource(R.drawable.resizable_textfield_error);
		}
	}
	
	private boolean isUsernameCorrect(String username) {
		if (getResources().getBoolean(R.bool.allow_only_phone_numbers_in_wizard)) {
			LinphoneProxyConfig lpc = LinphoneManager.getLc().createProxyConfig();
			return lpc.isPhoneNumber(username);
		} else {
			return username.matches("^[a-z]+[a-z0-9.\\-_]{2,}$");
		}
	}
	
	private void isUsernameRegistred(final String username, final ImageView icon) {
		final Runnable runNotOk = new Runnable() {
			public void run() {
				usernameOk = false;
				displayError(usernameOk, usernameError, usernameEdit, LinphoneManager.getInstance().getContext().getString(R.string.wizard_username_unavailable));
				createAccount.setEnabled(usernameOk && passwordOk && confirmPasswordOk && emailOk);
			}
		};
		final Runnable runOk = new Runnable() {
			public void run() {
				usernameOk = true;
				displayError(usernameOk, usernameError, usernameEdit, "");
				createAccount.setEnabled(usernameOk && passwordOk && confirmPasswordOk && emailOk);
			}
		};
		final Runnable runNotReachable = new Runnable() {
			public void run() {
				usernameOk = false;
				displayError(usernameOk, usernameError, usernameEdit, LinphoneManager.getInstance().getContext().getString(R.string.wizard_server_unavailable));
				createAccount.setEnabled(usernameOk && passwordOk && confirmPasswordOk && emailOk);
			}
		};
		
		LinphoneXmlRpcRequest xmlRpcRequest = new LinphoneXmlRpcRequestImpl("is_account_used", LinphoneXmlRpcRequest.ArgType.String);
		xmlRpcRequest.setListener(new LinphoneXmlRpcRequestListener() {
			@Override
			public void onXmlRpcRequestResponse(LinphoneXmlRpcRequest request) {
				if (request.getStatus() == LinphoneXmlRpcRequest.Status.Ok) {
					String response = request.getStringResponse();
					if(response.equals(XmlRpcHelper.SERVER_ERROR_ACCOUNT_DOESNT_EXIST)) {
						mHandler.post(runOk);
					} else {
						mHandler.post(runNotOk);
					}
				} else if (request.getStatus() == LinphoneXmlRpcRequest.Status.Failed) {
					mHandler.post(runNotReachable);
				}
			}
		});
		xmlRpcRequest.addStringArg(username);
		xmlRpcSession.sendRequest(xmlRpcRequest);
	}

	private void isPhoneNumberRegistred(final String username, final ImageView icon) {
		final Runnable runOk = new Runnable() {
			public void run() {
				phoneNumberOk = false;
				displayError(phoneNumberOk, phoneNumberError, phoneNumberEdit, LinphoneManager.getInstance().getContext().getString(R.string.assistant_phone_number_unavailable));
				//createAccount.setEnabled(phoneNumberOk);
			}
		};
		final Runnable runNotOk = new Runnable() {
			public void run() {
				phoneNumberOk = true;
				displayError(phoneNumberOk, phoneNumberError, phoneNumberEdit, "");
				createAccount.setEnabled(phoneNumberOk);
			}
		};
		final Runnable runNotReachable = new Runnable() {
			public void run() {
				phoneNumberOk = false;
				displayError(phoneNumberOk, phoneNumberError, phoneNumberEdit, LinphoneManager.getInstance().getContext().getString(R.string.wizard_server_unavailable));
				createAccount.setEnabled(phoneNumberOk);
			}
		};

		LinphoneXmlRpcRequest xmlRpcRequest = new LinphoneXmlRpcRequestImpl("is_phone_number_used", LinphoneXmlRpcRequest.ArgType.String);
		xmlRpcRequest.setListener(new LinphoneXmlRpcRequestListener() {
			@Override
			public void onXmlRpcRequestResponse(LinphoneXmlRpcRequest request) {
				if (request.getStatus() == LinphoneXmlRpcRequest.Status.Ok) {
					final String response = request.getStringResponse();
					if (response.equals(XmlRpcHelper.SERVER_RESPONSE_OK)) {
						mHandler.post(runOk);
					} else {
						if(response.startsWith("ERROR")){
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									phoneNumberOk = false;
									displayError(phoneNumberOk, phoneNumberError, phoneNumberEdit, response);
									//createAccount.setEnabled(phoneNumberOk);
								}
							});
						} else {
							mHandler.post(runNotOk);
						}
					}
				} else if (request.getStatus() == LinphoneXmlRpcRequest.Status.Failed) {
					mHandler.post(runNotReachable);
				}
			}
		});
		xmlRpcRequest.addStringArg(username);
		xmlRpcSession.sendRequest(xmlRpcRequest);
	}
	
	private boolean isEmailCorrect(String email) {
    	Pattern emailPattern = Patterns.EMAIL_ADDRESS;
    	return emailPattern.matcher(email).matches();
	}
	
	private boolean isPasswordCorrect(String password) {
		return password.length() >= 1;
	}
	
	private void createAccountWithPhoneNumber(final String username, final String password, final String phone, boolean suscribe) {
		AssistantActivity.instance().displayAssistantCodeConfirm(username, phone);
		final Runnable runNotOk = new Runnable() {
			public void run() {
				//TODO errorMessage.setText(R.string.wizard_failed);
			}
		};
		final Runnable runOk = new Runnable() {
			public void run() {
				AssistantActivity.instance().displayAssistantCodeConfirm(username, phone);
			}
		};
		final Runnable runNotReachable = new Runnable() {
			public void run() {
				//TODO errorMessage.setText(R.string.wizard_not_reachable);
			}
		};
		
		LinphoneXmlRpcRequest xmlRpcRequest = new LinphoneXmlRpcRequestImpl("create_phone_account", LinphoneXmlRpcRequest.ArgType.String);
		xmlRpcRequest.setListener(new LinphoneXmlRpcRequestListener() {
			@Override
			public void onXmlRpcRequestResponse(LinphoneXmlRpcRequest request) {
				if (request.getStatus() == LinphoneXmlRpcRequest.Status.Ok) {
					String response = request.getStringResponse();
					if (response.contains("ERROR")) {
						mHandler.post(runNotOk);
					} else {
						mHandler.post(runOk);
					}
				} else if (request.getStatus() == LinphoneXmlRpcRequest.Status.Failed) {
					mHandler.post(runNotReachable);
				}
			}
		});
		xmlRpcRequest.addStringArg(phone);
		xmlRpcRequest.addStringArg("");
		xmlRpcRequest.addStringArg("");
		xmlRpcRequest.addStringArg(LinphoneManager.getInstance().getUserAgent());
		xmlRpcRequest.addStringArg("");
		xmlRpcSession.sendRequest(xmlRpcRequest);
	}

	private void createAccountWithEmail(final String username, final String password, String email, boolean suscribe) {
		final Runnable runNotOk = new Runnable() {
			public void run() {
				//TODO errorMessage.setText(R.string.wizard_failed);
			}
		};
		final Runnable runOk = new Runnable() {
			public void run() {
				AssistantActivity.instance().displayAssistantConfirm(username, password);
			}
		};
		final Runnable runNotReachable = new Runnable() {
			public void run() {
				//TODO errorMessage.setText(R.string.wizard_not_reachable);
			}
		};

		LinphoneXmlRpcRequest xmlRpcRequest = new LinphoneXmlRpcRequestImpl("create_account_with_useragent", LinphoneXmlRpcRequest.ArgType.Int);
		xmlRpcRequest.setListener(new LinphoneXmlRpcRequestListener() {
			@Override
			public void onXmlRpcRequestResponse(LinphoneXmlRpcRequest request) {
				if (request.getStatus() == LinphoneXmlRpcRequest.Status.Ok) {
					int response = request.getIntResponse();
					if (response != 0) {
						mHandler.post(runNotOk);
					} else {
						mHandler.post(runOk);
					}
				} else if (request.getStatus() == LinphoneXmlRpcRequest.Status.Failed) {
					mHandler.post(runNotReachable);
				}
			}
		});
		xmlRpcRequest.addStringArg(username);
		xmlRpcRequest.addStringArg(password);
		xmlRpcRequest.addStringArg(email);
		xmlRpcRequest.addStringArg(LinphoneManager.getInstance().getUserAgent());
		xmlRpcSession.sendRequest(xmlRpcRequest);
	}

	private void addXMLRPCPhoneNumberHandler(final EditText field, final ImageView icon) {
		field.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (s.length() > 0) {
					phoneNumberOk = false;
					String phoneNumber = getPhoneNumber();
					if (LinphoneManager.getLc().createProxyConfig().isPhoneNumber(phoneNumber)) {
						isPhoneNumberRegistred(phoneNumber, icon);
					} else {
						displayError(phoneNumberOk, phoneNumberError, phoneNumberEdit, getResources().getString(R.string.wizard_username_incorrect));
					}
				} else {
					displayError(phoneNumberOk, phoneNumberError, phoneNumberEdit, "");
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			public void onTextChanged(CharSequence s, int start, int count, int after) {}
		});
	}
	
	private void addXMLRPCUsernameHandler(final EditText field, final ImageView icon) {
		field.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				Matcher matcher = UPPER_CASE_REGEX.matcher(s);
				while (matcher.find()) {
					CharSequence upperCaseRegion = s.subSequence(matcher.start(), matcher.end());
					s.replace(matcher.start(), matcher.end(), upperCaseRegion.toString().toLowerCase());
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}

			public void onTextChanged(CharSequence s, int start, int count, int after) {
				field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (!hasFocus) {
							usernameOk = false;
							String username = field.getText().toString();
							if (isUsernameCorrect(username)) {
								if (getResources().getBoolean(R.bool.allow_only_phone_numbers_in_wizard)) {
									LinphoneProxyConfig lpc = LinphoneManager.getLc().createProxyConfig();
									username = lpc.normalizePhoneNumber(username);
								}
								isUsernameRegistred(username, icon);
							} else {
								displayError(usernameOk, usernameError, usernameEdit, getResources().getString(R.string.wizard_username_incorrect));
							}
						} else {
							displayError(true, usernameError, usernameEdit, "");
						}
					}
				});
			}
		});
	}
	
	private void addXMLRPCEmailHandler(final EditText field, final ImageView icon) {
		field.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}

			public void onTextChanged(CharSequence s, int start, int count, int after) 
			{
				emailOk = false;
				if (isEmailCorrect(field.getText().toString())) {
					emailOk = true;
					displayError(emailOk, emailError, emailEdit, "");
				}
				else {
					displayError(emailOk, emailError, emailEdit, getString(R.string.wizard_email_incorrect));
				}
				createAccount.setEnabled(usernameOk && passwordOk && confirmPasswordOk && emailOk);
			}
		});
	}
	
	private void addXMLRPCPasswordHandler(final EditText field1, final ImageView icon) {
		TextWatcher passwordListener = new TextWatcher() {
			public void afterTextChanged(Editable s) {
				
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}

			public void onTextChanged(CharSequence s, int start, int count, int after) 
			{
				passwordOk = false;
				if (isPasswordCorrect(field1.getText().toString())) {
					passwordOk = true;
					displayError(passwordOk, passwordError, passwordEdit, "");
				}
				else {
					displayError(passwordOk, passwordError, passwordEdit, getString(R.string.wizard_password_incorrect));
				}
				createAccount.setEnabled(usernameOk && passwordOk && confirmPasswordOk && emailOk);
			}
		};
		
		field1.addTextChangedListener(passwordListener);
	}
	
	private void addXMLRPCConfirmPasswordHandler(final EditText field1, final EditText field2, final ImageView icon) {
		TextWatcher passwordListener = new TextWatcher() {
			public void afterTextChanged(Editable s) {
				
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
			}

			public void onTextChanged(CharSequence s, int start, int count, int after) 
			{
				field2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (!hasFocus) {
							confirmPasswordOk = false;
							if (field1.getText().toString().equals(field2.getText().toString())) {
								confirmPasswordOk = true;

								if (!isPasswordCorrect(field1.getText().toString())) {
									displayError(passwordOk, passwordError, passwordEdit, getString(R.string.wizard_password_incorrect));
								} else {
									displayError(confirmPasswordOk, passwordConfirmError, passwordConfirmEdit, "");
								}
							} else {
								displayError(confirmPasswordOk, passwordConfirmError, passwordConfirmEdit, getString(R.string.wizard_passwords_unmatched));
							}
							createAccount.setEnabled(usernameOk && passwordOk && confirmPasswordOk && emailOk);
						} else {
							displayError(true, passwordConfirmError, passwordConfirmEdit, "");
						}
					}
				});

			}
		};
		
		field1.addTextChangedListener(passwordListener);
		field2.addTextChangedListener(passwordListener);
	}
}
