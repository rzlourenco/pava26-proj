(defmacro def-class (class &rest fields)
  (if (atom class) (setf class (list class)))
  (flet ((make-name (&rest names)
                    (intern (format nil "~{~a~}" names)))
         (make-keyword (&rest names)
                    (intern (format nil "~{~a~}" names) "KEYWORD")))
  (flet
    ((get-fields (class)
       (let ((constructor-name (make-name "MAKE-" class)))
         (copy-list (get constructor-name :args))))
     (call-superclass-constructor (superclass params all-args)
       (let ((constructor-name (make-name "MAKE-" superclass))
             (args (intersection all-args params)))
         `(,constructor-name
            ,@(apply #'append
                (mapcar
                  (lambda (arg) (list (make-keyword arg) arg))
                  args))))))
  (let* ((classname (first class))
         (superclasses (rest class))
         (superclass-field-pairs
           (mapcar
             (lambda (class)
               (let ((fields (get-fields class)))
                 `(,class . ,fields)))
             superclasses))
         (all-fields
           (remove-duplicates
             (append fields
               (apply #'append
                 (mapcar #'cdr superclass-field-pairs)))))
         (constructor-name (make-name "MAKE-" classname)))
  (flet
    ((make-constructor ()
       `(defun ,constructor-name (&key ,@all-fields)
          (let ((sym (gensym))
                (next 
                  ,(when superclasses
                      (reduce
                        (lambda (acc e)
                          (let ((symb (gensym)))
                            `(let ((,symb ,e))
                               (setf (get ,symb :next) ,acc)
                               ,symb)))
                        (mapcar
                          (lambda (pair)
                            (call-superclass-constructor (car pair)
                                                         (cdr pair)
                                                         all-fields))
                          (reverse superclass-field-pairs))))))
            (setf (get sym :next) next)
            (setf (get sym :type) ',classname)
            (setf (get sym :fields)
                  (list ,@(mapcar
                            (lambda (e) `(cons ',e ,e))
                            fields)))
            sym)))
     (make-predicate ()
       (let ((predicate-name (make-name classname "?")))
       `(defun ,predicate-name (arg)
          (cond ((not (symbolp arg)) nil)
                ((eq (get arg :type) ',classname) t)
                (arg (,predicate-name (get arg :next)))))))
     (make-getter (field)
       (let ((getter-name (make-name classname "-" field)))
       `(defun ,getter-name (arg)
          (let ((lel arg))
            (loop
              (pprint (list (get lel :type) ',field (get lel :fields)))
              (when (not lel)
                (return-from ,getter-name))
              (let ((f (assoc ',field (get lel :fields))))
                (when f
                  (return-from ,getter-name (cdr f)))
                (setf lel (get lel :next))))))))
    )

    `(progn
       ,@(mapcar #'make-getter all-fields)
       ,(make-predicate)
       ,(make-constructor)
       (setf (get ',constructor-name :args) ',all-fields)
       nil)
    )))))

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
