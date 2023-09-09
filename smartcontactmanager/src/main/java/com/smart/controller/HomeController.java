package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;

@Controller
public class HomeController {
	
	@Autowired
	private EmailService emailService;
	
	Random random=new Random(100000);
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;

	@GetMapping("/")
	public String home(Model m) {
		m.addAttribute("title","Home- Smart Contact Manager");
	
		return "home";
	}
	
	@GetMapping("/about")
	public String about(Model m) {
		m.addAttribute("title","About- Smart Contact Manager");
	
		return "about";
	}
	
	@GetMapping("/signUp")
	public String signUp(Model m) {
		m.addAttribute("title","Register- Smart Contact Manager");
		m.addAttribute("user", new User());
		return "signUp";
	}
	
	@GetMapping("/signin")
	public String customLogin(Model m) {
		m.addAttribute("title","Login- Smart Contact Manager");
		return "login";
	}
	
	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult bindingresult,@RequestParam(value="agreement",defaultValue="false") boolean agreement,Model m,HttpSession session) {

		try {
			if(!agreement) {
				System.out.println("You have not agreed the terms and conditions");
				throw new Exception("You have not agreed the terms and conditions");
			}
			
			if(bindingresult.hasErrors()) {
				System.out.println("ERROR:"+bindingresult.toString());
				m.addAttribute("user",user);
				return "signUp";
			}
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		
			User result=userRepository.save(user);
			System.out.println(result);
			
			m.addAttribute("user",new User());
			
			m.addAttribute("message",new Message("Successfully Registered !!","alert-success"));
			
			return "signUp";
		}
		catch(Exception e) {
			e.printStackTrace();
			m.addAttribute("user",user);
			m.addAttribute("message",new Message("Something went wrong !!","alert-danger"));
			return "signUp";
		}
		
	}
	
	@GetMapping("/forgot")
	public String emailForm() {
		return "forgot_pwd_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email,HttpSession session) {
		System.out.println(email);
		
				
		int otp=random.nextInt(1000000);
		
		System.out.println(otp);
		
		String subject="OTP For Email Verification";
		String message=""
				+"<div style='border:1px solid #e2e2e2;padding:20px;'>"
				+"<h1>"
				+"OTP is: "
				+"<b>"+otp
				+"</h1>"
				+"</div>";
		
		String to=email;
		
		boolean flag=this.emailService.sendEmail(subject, message, to);
		
		if(flag) {
			session.setAttribute("myotp",otp);
			session.setAttribute("email",email);
			return "verify_otp";
		}
		else {
			session.setAttribute("message",new Message("Incorrect Email id...!!","danger"));
			return "forgot_pwd_email_form";
		}
	}
	
	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("otp") int otp,HttpSession session) {
		
		int myotp=(int) session.getAttribute("myotp");
		
		String email=(String) session.getAttribute("email");
		
		if(myotp==otp) {
			
			User user=this.userRepository.getUserByUserName(email);
			
			if(user==null) {
				session.setAttribute("message",new Message("No user with this email...!!","danger"));
				return "forgot_pwd_email_form";
			}
			else {
				return "password_change_form";
			}
			
		}
		else {
			session.setAttribute("message", new Message("Please Check your OTP","danger"));
			return "verify_otp";
		}
	}
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newPassword,HttpSession session) {
		String email=(String)session.getAttribute("email");
		User user=this.userRepository.getUserByUserName(email);
		user.setPassword(this.passwordEncoder.encode(newPassword));
		this.userRepository.save(user);
		return "redirect:signin?change=Password changed successfully";
		
	}
	
}

