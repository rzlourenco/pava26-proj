(defmacro def-class (class &rest fields)
  (if (atom class) (setf class (list class)))
  (flet ((make-name (&rest names)
                    (intern (format nil "~{~a~}" names))))
  (flet
    ((get-fields (class)
       (let ((constructor-name (make-name "MAKE-" class)))
         (copy-list (get constructor-name :args)))))
  (let* ((classname (first class))
         (superclasses (rest class))
         (superclass-fields
           (apply #'nconc (mapcar #'get-fields superclasses)))
         (all-fields (append fields superclass-fields))
         (constructor-name (make-name "MAKE-" classname)))
  (flet
    ((make-constructor ()
       `(defun ,constructor-name (&key ,@all-fields)
          nil))
     (make-predicate ()
       `(defun ,(make-name classname "?") (arg)
          nil))
     (make-getter (field)
       `(defun ,(make-name classname "-" field) (arg)
          nil))
     )

    `(progn
       ,@(mapcar #'make-getter all-fields)
       ,(make-predicate)
       ,(make-constructor)
       (setf (get ',constructor-name :args) ',fields)
       nil)
    )))))

(pprint
  (macroexpand `(def-class person name age)))
(pprint
  (macroexpand `(def-class (student person) course)))
(pprint
  (macroexpand `(def-class sportsman activity schedule)))
(pprint
  (macroexpand `(def-class (ist-student student sportsman))))

(def-class person name age)
(def-class (student person) course) 
(def-class sportsman activity schedule)
(def-class (ist-student student sportsman))

;(let ((a (make-person :name "Paulo" :age 33))
;      (b "I am not a person"))
;  (pprint (list (person? a) (person? b))))
;
;(let ((s (make-student :name "Paul" :age 21 :course "Informatics")))
;  (pprint (list (student-name s) (student-course s))))
;
;(let ((p (make-person :name "John" :age 34))
;      (s (make-student :name "Paul" :age 21 :course "Informatics")))
;  (pprint (list (person? p) (student? p) (person? s) (student? s))))
;
;(let ((m (make-ist-student :name "Maria" :course "IA" :activity "Tennis")))
;  (pprint (list (ist-student? m)
;                (student? m)
;                (sportsman? m)
;                (ist-student-name m)
;                (person-name m)
;                (sportsman-activity m)
;                (ist-student-activity m))))
