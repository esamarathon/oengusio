package app.oengus.entity.model;

import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Set;

import static javax.persistence.CascadeType.*;

@Entity
@Table(name = "question")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Question {

	@Id
	@JsonView(Views.Public.class)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne
	@JoinColumn(name = "marathon_id")
	@JsonBackReference(value = "marathonReference")
	@JsonView(Views.Public.class)
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private Marathon marathon;

    @OneToMany(mappedBy = "id", cascade = { REMOVE, REFRESH, MERGE, DETACH })
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonView(Views.Internal.class)
    private Set<Answer> answers;

	@Column(name = "label")
	@JsonView(Views.Public.class)
	@Size(max = 50)
	private String label;

	@Column(name = "field_type")
	@JsonView(Views.Public.class)
	@NotNull
	private FieldType fieldType;

	@Column(name = "required")
	@JsonView(Views.Public.class)
	private boolean required;

	@ElementCollection
	@CollectionTable(name = "select_option", joinColumns = @JoinColumn(name = "question_id"))
	@JsonView(Views.Public.class)
	@Column(name = "question_option")
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private List<@Size(max = 50) String> options;

	@Column(name = "question_type")
	@JsonView(Views.Public.class)
	@NotBlank
	@Size(max = 10)
	private String questionType;

	@Column(name = "description")
	@JsonView(Views.Public.class)
	@Size(max = 1000)
	private String description;

	@Column(name = "position")
	@JsonView(Views.Public.class)
	private int position;

	public int getId() {
		return this.id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public Marathon getMarathon() {
		return this.marathon;
	}

	public void setMarathon(final Marathon marathon) {
		this.marathon = marathon;
	}

    public Set<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(Set<Answer> answers) {
        this.answers = answers;
    }

    public String getLabel() {
		return this.label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public FieldType getFieldType() {
		return this.fieldType;
	}

	public void setFieldType(final FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public boolean isRequired() {
		return this.required;
	}

	public void setRequired(final boolean required) {
		this.required = required;
	}

	public List<String> getOptions() {
		return this.options;
	}

	public void setOptions(final List<String> options) {
		this.options = options;
	}

	public String getQuestionType() {
		return this.questionType;
	}

	public void setQuestionType(final String questionType) {
		this.questionType = questionType;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public int getPosition() {
		return this.position;
	}

	public void setPosition(final int position) {
		this.position = position;
	}
}
