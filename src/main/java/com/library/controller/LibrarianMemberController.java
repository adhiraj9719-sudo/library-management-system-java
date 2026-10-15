package com.library.controller;

import com.library.dto.MemberForm;
import com.library.model.User;
import com.library.service.LibraryException;
import com.library.service.MemberService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/librarian/members")
public class LibrarianMemberController {

    private final MemberService members;

    public LibrarianMemberController(MemberService members) {
        this.members = members;
    }

    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "") String q, Model model) {
        List<User> found = members.list(q);
        Map<Long, Long> loanCounts = new LinkedHashMap<>();
        for (User u : found) {
            loanCounts.put(u.getId(), members.activeLoans(u.getId()));
        }
        model.addAttribute("members", found);
        model.addAttribute("loanCounts", loanCounts);
        model.addAttribute("q", q);
        return "librarian/members";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new MemberForm());
        model.addAttribute("editing", false);
        return "librarian/member-form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") MemberForm form, Model model, RedirectAttributes flash) {
        try {
            User user = members.create(form);
            flash.addFlashAttribute("flashSuccess", "Member " + user.getName() + " created.");
            return "redirect:/librarian/members";
        } catch (LibraryException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", false);
            return "librarian/member-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("form", MemberForm.of(members.get(id)));
            model.addAttribute("editing", true);
            model.addAttribute("memberId", id);
            return "librarian/member-form";
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/librarian/members";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("form") MemberForm form, Model model,
                         RedirectAttributes flash) {
        try {
            User user = members.update(id, form);
            flash.addFlashAttribute("flashSuccess", "Member " + user.getName() + " updated.");
            return "redirect:/librarian/members";
        } catch (LibraryException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("editing", true);
            model.addAttribute("memberId", id);
            return "librarian/member-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            String name = members.delete(id);
            flash.addFlashAttribute("flashSuccess", "Member " + name + " deleted.");
        } catch (LibraryException e) {
            flash.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/librarian/members";
    }
}
