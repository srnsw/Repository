// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package au.gov.nsw.records.digitalarchive.repo.model;

import au.gov.nsw.records.digitalarchive.repo.model.Repository;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

privileged aspect Repository_Roo_Jpa_Entity {
    
    declare @type: Repository: @Entity;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long Repository.id;
    
    @Version
    @Column(name = "version")
    private Integer Repository.version;
    
    public Long Repository.getId() {
        return this.id;
    }
    
    public void Repository.setId(Long id) {
        this.id = id;
    }
    
    public Integer Repository.getVersion() {
        return this.version;
    }
    
    public void Repository.setVersion(Integer version) {
        this.version = version;
    }
    
}
