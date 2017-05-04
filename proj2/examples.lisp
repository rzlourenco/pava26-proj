(load (compile-file "load.lisp"))

;(pprint
;  (macroexpand `(def-class person name age)))
(def-class person name age)
;(pprint
;  (macroexpand `(def-class (student person) course)))
(def-class (student person) course)
;(pprint
;  (macroexpand `(def-class sportsman activity schedule)))
(def-class sportsman activity schedule)
;(pprint
;  (macroexpand `(def-class (ist-student student sportsman))))
(def-class (ist-student student sportsman))
;(pprint
;  (macroexpand `(def-class (hero person) name power)))
(def-class (hero person) name power)

(format t "~%")
(let ((a (make-person :name "Paulo" :age 33))
      (b "I am not a person"))
  (pprint (list (person? a) (person? b))))

(let ((s (make-student :name "Paul" :age 21 :course "Informatics")))
  (pprint (list (student-name s) (student-course s))))

(let ((p (make-person :name "John" :age 34))
      (s (make-student :name "Paul" :age 21 :course "Informatics")))
  (pprint (list (person? p) (student? p) (person? s) (student? s))))

(let ((m (make-ist-student :name "Maria" :course "IA" :activity "Tennis")))
  (pprint (list (ist-student? m) (student? m) (sportsman? m) (ist-student-name m) (person-name m) (sportsman-activity m) (ist-student-activity m))))

(let ((m (make-hero :name "Coldsteel" :age 30 :power "edgyness")))
  (pprint (list (person-name m) (hero-name m)))
  (setf (person-name m) "Sonic")
  (pprint (list (person-name m) (hero-name m))))
