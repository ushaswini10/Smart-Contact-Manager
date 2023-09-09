package com.smart.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.smart.entities.*;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	
	// method for adding the common data to the response
	@ModelAttribute
	public void addCommonData(Model m,Principal principal) {
		String userName=principal.getName();
		
		User user=userRepository.getUserByUserName(userName);
		
		m.addAttribute("user",user);
	}

	@RequestMapping("/index")
	public String dashboard(Model m,Principal principal){
		m.addAttribute("title","User Dashboard- Smart Contact Manager");
		
		return "normal/user_dashboard";
	}
	
	@RequestMapping("/add-contact")
	public String openAddContactForm(Model m) {
		
		m.addAttribute("title","Add Contact- Smart Contact Manager");
		m.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal,
			HttpSession session) {
		
		try {
			String userName=principal.getName();
			User user=userRepository.getUserByUserName(userName);
			
			if(file.isEmpty()) {
				System.out.print("File is Empty");
				contact.setImage("profile.png");
			}
			else {
				
				// save the file to contact and upload the name to database
				contact.setImage(file.getOriginalFilename());
				
				File savefile=new ClassPathResource("static/img/").getFile();
				
				Path path=Paths.get(savefile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.print("Image Uploaded");
			}
			
			contact.setUser(user);
			user.getContacts().add(contact);
			
			userRepository.save(user);
			
			session.setAttribute("message", new Message("Contact is added successfully !! Add more","success"));
		}
		catch(Exception e) {
			e.printStackTrace();
			
			session.setAttribute("message", new Message("Something went wrong..Try again !!","danger"));
		}
		
		return "normal/add_contact_form";
	}
	
	//show contacts handler
	
	@RequestMapping("/show-contacts/{page}")
	public String showContacts(Model m,Principal principal,@PathVariable("page") Integer page) {
		
		m.addAttribute("title","Show Contact- Smart Contact Manager");
		
		String userName=principal.getName();
		
		User user=userRepository.getUserByUserName(userName);
		
		// current page , how many contacts per page
		Pageable pageable=PageRequest.of(page,4);
		
		Page<Contact> contacts=this.contactRepository.findContactsByUser(user.getId(),pageable);
		
		m.addAttribute("contacts",contacts);
		m.addAttribute("userId",user.getId());
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	@RequestMapping("/contact/{cid}")
	public String showContactDetails(Model m,@PathVariable("cid") Integer cid,Principal principal) {
		
		String userName=principal.getName();
		
		User user=userRepository.getUserByUserName(userName);
		
		
		Optional<Contact> contact=contactRepository.findById(cid);
		
		Contact c=contact.get();
		
		if(user.getId()==c.getUser().getId()) {
			m.addAttribute("contact",c);
			m.addAttribute("title",c.getName());
		}
		
		return "normal/contact_detail";
	}
	
	
	@RequestMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cid,Model m,HttpSession session) {
		
		m.addAttribute("title","Delete Contact- Smart Contact Manager");
		
		Optional<Contact> contact=contactRepository.findById(cid);		
		Contact c=contact.get();		
		
		//if you need to check...then follow the above process if user.getId()==c.getUser().getId()
		
		c.setUser(null);  // breaking the link of user and the contact to be deleted
		
		// if u need, u can delete the profile photo....in img folder and image name is c.getImage()
		// first make the path static/img/contact.getImage() and then delete the file
		contactRepository.delete(c);
		
		session.setAttribute("message",new Message("Contact deleted succesfully...!!","success"));
		
		return "redirect:/user/show-contacts/0";
	}
	
	@PostMapping("/update-contact/{cid}")
	public String updateContact(@PathVariable("cid") Integer cid,Model m) {
		
		m.addAttribute("title","Update Contact- Smart Contact Manager");
		
		Contact contact=contactRepository.findById(cid).get();
		
		m.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	
	@PostMapping("/process-update")
	public String processUpdate(@ModelAttribute Contact contact,Model m,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal,
			HttpSession session) {
		
		m.addAttribute("title","Processing Update- Smart Contact Manager");
		
		try {
			
			//old contact details
			Contact oldContact=this.contactRepository.findById(contact.getCid()).get();
			if(!file.isEmpty()) {
				
				// update image
				// delete old photo
				
				File deletefile=new ClassPathResource("static/img/").getFile();
				
				File file1=new File(deletefile,oldContact.getImage());
				
				file1.delete();
				//update new photo 
				File savefile=new ClassPathResource("static/img/").getFile();
				
				Path path=Paths.get(savefile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}
			else {
				contact.setImage(oldContact.getImage());
			}
			
			User user=this.userRepository.getUserByUserName(principal.getName());
			
			contact.setUser(user);
			
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Contact updated successfully","success"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return "redirect:/user/contact/"+contact.getCid();
	}
	
	//profile
	@GetMapping("/profile")
	public String profile(Model m) {
		
		m.addAttribute("title","Your profile- Smart Contact Manager");
		
		return "normal/profile";
	}
	
	//settings
	@GetMapping("/settings")
	public String settings(Model m) {
		
		m.addAttribute("Settings","Your profile- Smart Contact Manager");
		
		return "normal/settings";
	}
	
	//change password
	@PostMapping("/change-password")
	public String changePassword(Model m,
			@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword,
			@RequestParam("confirmNewPassword") String confirmNewPassword,
			Principal principal,HttpSession session) {
		
		m.addAttribute("Change Password","Your profile- Smart Contact Manager");
		
		String userName=principal.getName();
		
		User user=userRepository.getUserByUserName(userName);
		
		if(this.bCryptPasswordEncoder.matches(oldPassword,user.getPassword())) {
			if(newPassword.equals(confirmNewPassword)) {
				user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
				this.userRepository.save(user);
				session.setAttribute("message", new Message("Password has been changed successfully...!!","success"));
			}
			else {
				session.setAttribute("message", new Message("Something went wrong...!!","danger"));
				
				return "redirect:/user/settings";
			}
		}
		else {
			session.setAttribute("message", new Message("Please correct your old password...!!","danger"));
			
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}
	
	
}
