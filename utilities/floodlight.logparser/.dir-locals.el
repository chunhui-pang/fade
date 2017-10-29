;; add project-root to company-clang-argument
((c++-mode . ((eval . (let ((include-args
						(concat "-I"
								(file-name-directory
								 (let ((d (dir-locals-find-file ".")))
								   (if (stringp d) d (car d)))))))
						(progn
						  (message "set compan-clang-arguments to '%s'." include-args)
						  (setq company-clang-arguments (list include-args))))))))

