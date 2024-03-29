package app.oengus.entity.model;

import app.oengus.entity.comparator.AnswerComparator;
import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Size;

@Entity
@Table(name = "answer")
public class Answer implements Comparable<Answer> {

	@Id
	@JsonView(Views.Public.class)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne
	@JoinColumn(name = "question_id")
	@JsonView(Views.Public.class)
	private Question question;

	@ManyToOne
	@JoinColumn(name = "submission_id")
	@JsonBackReference(value = "answersReference")
	@JsonView(Views.Public.class)
	private Submission submission;

	@Column(name = "answer")
	@JsonView(Views.Public.class)
	@Size(max = 500)
	private String answer;

	@AssertTrue
	public boolean isAnswerRequired() {
		if (this.question == null) { // WHY IS QUESTION NULL
			return false;
		}

		if (!this.question.isRequired()) {
			return true;
		}

		return StringUtils.isNotEmpty(this.answer);
	}

	public int getId() {
		return this.id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public Question getQuestion() {
		return this.question;
	}

	public void setQuestion(final Question question) {
		this.question = question;
	}

	public Submission getSubmission() {
		return this.submission;
	}

	public void setSubmission(final Submission submission) {
		this.submission = submission;
	}

	public String getAnswer() {
		return this.answer;
	}

	public void setAnswer(final String answer) {
		this.answer = answer;
	}

	@Override
	public int compareTo(final Answer o) {
		return new AnswerComparator().compare(this, o);
	}
}
